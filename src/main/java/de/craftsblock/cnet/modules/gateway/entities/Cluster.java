package de.craftsblock.cnet.modules.gateway.entities;

import de.craftsblock.craftscore.utils.id.Snowflake;
import de.craftsblock.craftsnet.api.utils.Scheme;
import de.craftsblock.craftsnet.utils.ByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cluster extends Entity {

    private final @NotNull String base;
    private final @NotNull String domain;

    private final @NotNull Matcher baseMatcher;
    private final @NotNull Matcher domainMatcher;

    private final ConcurrentHashMap<Long, ClusterChild> children = new ConcurrentHashMap<>();

    public Cluster(@NotNull String base, @NotNull String domain) {
        this(Snowflake.generate(), base, domain);
    }

    public Cluster(@NotNull ByteBuffer buffer) {
        this(buffer.readLong(), buffer.readUTF(), buffer.readUTF());

        int size = buffer.readInt();
        for (int i = 0; i < size; i++) {
            ClusterChild child = new ClusterChild(this, buffer);
            children.put(child.getIdLong(), child);
        }
    }

    private Cluster(long id, @NotNull String base, @NotNull String domain) {
        super(id);

        this.base = base;
        this.domain = domain;

        this.baseMatcher = Pattern.compile(sanitizeBasePattern(base)).matcher("");
        this.domainMatcher = Pattern.compile(sanitizeBasePattern(domain)).matcher("");
    }

    public Cluster registerChild(@NotNull Scheme scheme, @NotNull String host, int port) {
        return this.registerChild(scheme, host, port, "/");
    }

    public Cluster registerChild(@NotNull Scheme scheme, @NotNull String host, int port, @NotNull String base) {
        ClusterChild child = new ClusterChild(this, scheme, host, port, base);
        this.children.put(child.getIdLong(), child);
        return this;
    }

    public Cluster unregisterChild(ClusterChild child) {
        return this.unregisterChild(child.getIdLong());
    }

    public Cluster unregisterChild(long id) {
        this.children.remove(id);
        return this;
    }

    private String sanitizeBasePattern(final String base) {
        final String pattern = base.trim();
        final StringBuilder builder = new StringBuilder(pattern.replaceFirst("(\\s+)?\\^/|(\\s+)?\\^|(\\s+)?/", ""));

        builder.insert(0, "^/");
        if (pattern.endsWith("$")) builder.deleteCharAt(builder.length() - 1);
        if (pattern.endsWith("/")) builder.deleteCharAt(builder.length() - 1);

        return builder + "(?<path>.*)$";
    }

    public String removeBaseFromPath(String path) {
        Matcher matcher = this.baseMatcher.reset(path);
        if (!matcher.matches()) return null;
        return matcher.group("path");
    }

    public boolean isBaseApplicable(String path) {
        return this.baseMatcher.reset(path).matches();
    }

    public @NotNull String getBase() {
        return this.base;
    }

    public boolean isDomainApplicable(String domain) {
        return this.domainMatcher.reset(domain).matches();
    }

    public @NotNull String getDomain() {
        return this.domain;
    }

    public @NotNull @Unmodifiable Collection<ClusterChild> getChildren() {
        return Collections.unmodifiableCollection(children.values());
    }

    @Override
    public void write(@NotNull ByteBuffer buffer) {
        super.write(buffer);

        buffer.writeUTF(this.base);
        buffer.writeUTF(this.domain);

        buffer.writeInt(children.size());
        children.values().forEach(child -> child.write(buffer));
    }

}
