package me.jiangcai.lib.embedweb.host;

import me.jiangcai.lib.embedweb.EmbedWeb;
import me.jiangcai.lib.embedweb.host.service.EmbedWebInfoService;
import me.jiangcai.lib.embedweb.thymeleaf.EWPProcessorDialect;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.access.BootstrapException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * 继承该配置或者引入该配置表示这是一个web宿主
 *
 * @author CJ
 */
@SuppressWarnings({"WeakerAccess", "SpringFacetCodeInspection"})
@Configuration
@ComponentScan({"me.jiangcai.lib.embedweb.host.service", "me.jiangcai.lib.embedweb.thymeleaf"})
@EnableWebMvc
@EnableAspectJAutoProxy
public class WebHost extends WebMvcConfigurerAdapter implements BeanPostProcessor {

    private static final Log log = LogFactory.getLog(WebHost.class);
    @Autowired
    private EmbedWebInfoService embedWebInfoService;
    @Autowired
    private WebApplicationContext webApplicationContext;
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @Autowired(required = false)
    private Set<TemplateEngine> templateEngines;
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @Autowired(required = false)
    private Set<ThymeleafViewResolver> thymeleafViewResolvers;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        super.addResourceHandlers(registry);
        embedWebInfoService.embedWebInfoStream().forEach(embedWebInfo -> {
            registry.addResourceHandler(embedWebInfo.getPubicResourceUri() + "/**")
                    .addResourceLocations(embedWebInfo.getPubicResourceUri() + "/");
        });
    }

    /**
     * @return ViewNameAdjust instance
     */
    @Bean
    public ViewNameAdjust viewNameAdjust() {
        return new ViewNameAdjust();
    }

    @Autowired
    public void link(EWPProcessorDialect ewpProcessorDialect) {
        if (templateEngines == null && thymeleafViewResolvers == null) {
            log.warn("no TemplateEngine found, you should use\n\n@Autowired\nEWPProcessorDialect ewpProcessorDialect;\n" +
                    "into your TemplateEngine");
        } else {
            log.debug("setup EWPProcessorDialect");
            Consumer<TemplateEngine> consumer = templateEngine -> templateEngine.addDialect(ewpProcessorDialect);

            if (templateEngines != null)
                templateEngines.forEach(consumer);

            if (thymeleafViewResolvers != null)
                thymeleafViewResolvers.stream().map(thymeleafViewResolver
                        -> (TemplateEngine) thymeleafViewResolver.getTemplateEngine()).forEach(consumer);
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        assert webApplicationContext != null;
        // name-version=value

        if (bean instanceof EmbedWeb) {
            EmbedWeb web = (EmbedWeb) bean;

            log.debug("checking EWP " + web.name());
            try {
                String uuid = uuidFrom(web);
                if (uuid == null) {
                    // 需要部署资源
                    log.info(web.name() + " is installing.");
                    uuid = UUID.randomUUID().toString().replaceAll("-", "");

                    copyResource(uuid, "private", web.privateResourcePath(), web.getClass());
                    copyResource(uuid, "public", web.publicResourcePath(), web.getClass());

                    updateUuid(uuid, web);
                }
            } catch (IOException | URISyntaxException ex) {
                throw new BootstrapException("deploy resource failed on " + web.name(), ex);
            }
        }

        return bean;
    }

    private void copyResource(String uuid, String tag, String path, Class<? extends EmbedWeb> webClass)
            throws URISyntaxException, IOException {
        URL url = webClass.getResource(path);
        if (url == null) {
            log.debug("no resources find for " + tag + " from " + path);
            return;
        }
        String rootPath = webApplicationContext.getServletContext().getRealPath(uuid + "/" + tag);
        if (!new File(rootPath).mkdirs()) {
            throw new IOException("failed to mkdirs for " + rootPath);
        }

        URI uri = webClass.getResource(path).toURI();
        Path myPath;
        FileSystem fileSystem = null;
        try {

            if (uri.getScheme().equals("jar")) {
                fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                myPath = fileSystem.getPath(path);
            } else {
                myPath = Paths.get(uri);
            }
            Stream<Path> walk = Files.walk(myPath);

            walk
                    .forEach(filePath -> {
                        String name = filePath.toString().substring(myPath.toString().length());

                        log.debug("start copy resource " + filePath);

                        String targetPath = webApplicationContext.getServletContext().getRealPath(uuid + "/" + tag
                                + name);

                        try {
                            File targetFile = new File(targetPath);
                            Files.copy(filePath, Paths.get(targetFile.toURI()), REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new RuntimeException("failed to copy file " + filePath, e);
                        }

                    });

        } finally {
            if (fileSystem != null)
                //noinspection ThrowFromFinallyBlock
                fileSystem.close();
        }
    }

    private void updateUuid(String uuid, EmbedWeb web) throws IOException {
        File file = new File(webApplicationContext.getServletContext().getRealPath(".ewp.properties"));
        Properties properties = new Properties();
        if (file.exists())
            try (FileInputStream inputStream = new FileInputStream(file)) {
                properties.load(inputStream);
            }

        properties.setProperty(web.name() + "-" + web.version(), uuid);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            properties.store(outputStream, null);
            outputStream.flush();
        }
        embedWebInfoService.webUUIDs().put(web, uuid);
    }

    private String uuidFrom(EmbedWeb web) throws IOException {
        File file = new File(webApplicationContext.getServletContext().getRealPath(".ewp.properties"));
        if (!file.exists())
            return null;
        Properties properties = new Properties();
        try (FileInputStream inputStream = new FileInputStream(file)) {
            properties.load(inputStream);
        }

        String uuid = properties.getProperty(web.name() + "-" + web.version());
        if (uuid == null)
            return null;
        embedWebInfoService.webUUIDs().put(web, uuid);
        return uuid;
    }
}