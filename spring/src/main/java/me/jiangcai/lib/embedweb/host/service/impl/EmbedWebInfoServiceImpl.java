package me.jiangcai.lib.embedweb.host.service.impl;

import me.jiangcai.lib.embedweb.EmbedWeb;
import me.jiangcai.lib.embedweb.PathService;
import me.jiangcai.lib.embedweb.exception.NoSuchEmbedWebException;
import me.jiangcai.lib.embedweb.host.WebHost;
import me.jiangcai.lib.embedweb.host.model.EmbedWebInfo;
import me.jiangcai.lib.embedweb.host.service.EmbedWebInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletContext;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by luffy on 2016/5/6.
 *
 * @author luffy luffy.ja at gmail.com
 */
@Service
public class EmbedWebInfoServiceImpl implements EmbedWebInfoService, PathService {

    @Autowired
    private ServletContext servletContext;

    private final ThreadLocal<EmbedWebInfo> currentInfo = new ThreadLocal<>();
    private final List<EmbedWebInfo> webInfoList = new ArrayList<>();
    private final Map<EmbedWeb, String> UUIDs = new HashMap<EmbedWeb, String>() {
        @Override
        public String put(EmbedWeb key, String value) {
            String result = super.put(key, value);
            EmbedWebInfo info = new EmbedWebInfo();
            info.setUuid(value);
            info.setSource(key.getClass().getProtectionDomain().getCodeSource());
            info.setName(key.name());
            info.setVersion(key.version());
            info.setPrivateResourceUri("/" + WebHost.HeaderPrivate + "/" + value + "/private");
            info.setPubicResourceUri("/" + WebHost.HeaderPublic + "/" + value + "/public");
            webInfoList.add(info);
            return result;
        }
    };


    private EmbedWebInfo fromName(String name, String path) throws NoSuchEmbedWebException {
        if (name == null || path == null)
            throw new IllegalArgumentException("bad name or path");
        if (!path.startsWith("/"))
            throw new IllegalArgumentException("bad path:" + path);

        return webInfoList.stream().filter(embedWebInfo -> embedWebInfo.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new NoSuchEmbedWebException(name, null));
    }

    @Override
    public String forPrivate(String name, String path) throws NoSuchEmbedWebException {
        EmbedWebInfo info = fromName(name, path);
        return getPrivatePath(path, info);
    }

    private String getPrivatePath(String path, EmbedWebInfo info) {
        if (path.startsWith("/"))
            return info.getPrivateResourceUri() + path;
        return info.getPrivateResourceUri() + "/" + path;
    }

    @Override
    public String forPublic(String name, String path) throws NoSuchEmbedWebException {
        EmbedWebInfo info = fromName(name, path);
        return getPublicPath(path, info);
    }

    private String getPublicPath(String path, EmbedWebInfo info) {
        if (path.startsWith("/"))
            return info.getPubicResourceUri() + path;
        return info.getPubicResourceUri() + "/" + path;
    }


    @Override
    public void setupCurrentEmbedWeb(Class type) {
        currentInfo.set(fromType(type));
    }

    @Override
    public EmbedWebInfo getCurrentEmbedWebInfo() {
        return currentInfo.get();
    }

    private EmbedWebInfo fromType(Class type) {
        CodeSource source = type.getProtectionDomain().getCodeSource();
        return webInfoList.stream().filter(embedWebInfo -> embedWebInfo.getSource() == source)
                .findFirst()
                .orElse(null);
    }

    @Override
    public Map<EmbedWeb, String> webUUIDs() {
        return UUIDs;
    }

    @Override
    public Stream<EmbedWebInfo> embedWebInfoStream() {
        return webInfoList.stream();
    }

    @Override
    public String forPrivate(String path) throws NoSuchEmbedWebException {
        EmbedWebInfo info = getCurrentEmbedWebInfo();
        if (info == null)
            throw new NoSuchEmbedWebException("unknown", null);
        return getPrivatePath(path, info);
    }

    @Override
    public String forPublic(String path) throws NoSuchEmbedWebException {
        EmbedWebInfo info = getCurrentEmbedWebInfo();
        if (info == null)
            throw new NoSuchEmbedWebException("unknown", null);
        return getPublicPath(path, info);
    }

    @Override
    public String publicContentPath(String path) throws NoSuchEmbedWebException {
        return servletContext.getContextPath() + forPublic(path);
    }
}
