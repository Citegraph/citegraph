//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.apache.tinkerpop.gremlin.hadoop.process.computer;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.tinkerpop.gremlin.hadoop.structure.HadoopGraph;
import org.apache.tinkerpop.gremlin.hadoop.structure.util.ConfUtil;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.process.computer.GraphFilter;
import org.apache.tinkerpop.gremlin.process.computer.MapReduce;
import org.apache.tinkerpop.gremlin.process.computer.VertexProgram;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer.Exceptions;
import org.apache.tinkerpop.gremlin.process.computer.util.GraphComputerHelper;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.apache.tinkerpop.gremlin.util.Gremlin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHadoopGraphComputer implements GraphComputer {
    private static final Pattern PATH_PATTERN;
    protected final Logger logger;
    protected final HadoopGraph hadoopGraph;
    protected boolean executed = false;
    protected final Set<MapReduce> mapReducers = new HashSet();
    protected VertexProgram<Object> vertexProgram;
    protected int workers = 1;
    protected GraphComputer.ResultGraph resultGraph = null;
    protected GraphComputer.Persist persist = null;
    protected GraphFilter graphFilter = new GraphFilter();

    public AbstractHadoopGraphComputer(final HadoopGraph hadoopGraph) {
        this.hadoopGraph = hadoopGraph;
        this.logger = LoggerFactory.getLogger(this.getClass());
    }

    public GraphComputer vertices(final Traversal<Vertex, Vertex> vertexFilter) {
        this.graphFilter.setVertexFilter(vertexFilter);
        return this;
    }

    public GraphComputer edges(final Traversal<Vertex, Edge> edgeFilter) {
        this.graphFilter.setEdgeFilter(edgeFilter);
        return this;
    }

    public GraphComputer result(final GraphComputer.ResultGraph resultGraph) {
        this.resultGraph = resultGraph;
        return this;
    }

    public GraphComputer persist(final GraphComputer.Persist persist) {
        this.persist = persist;
        return this;
    }

    public GraphComputer program(final VertexProgram vertexProgram) {
        this.vertexProgram = vertexProgram;
        return this;
    }

    public GraphComputer mapReduce(final MapReduce mapReduce) {
        this.mapReducers.add(mapReduce);
        return this;
    }

    public GraphComputer workers(final int workers) {
        this.workers = workers;
        return this;
    }

    public String toString() {
        return StringFactory.graphComputerString(this);
    }

    protected void validateStatePriorToExecution() {
        if (this.executed) {
            throw Exceptions.computerHasAlreadyBeenSubmittedAVertexProgram();
        } else {
            this.executed = true;
            if (null == this.vertexProgram && this.mapReducers.isEmpty()) {
                throw Exceptions.computerHasNoVertexProgramNorMapReducers();
            } else {
                if (null != this.vertexProgram) {
                    GraphComputerHelper.validateProgramOnComputer(this, this.vertexProgram);
                    this.mapReducers.addAll(this.vertexProgram.getMapReducers());
                }

                this.persist = GraphComputerHelper.getPersistState(Optional.ofNullable(this.vertexProgram), Optional.ofNullable(this.persist));
                this.resultGraph = GraphComputerHelper.getResultGraphState(Optional.ofNullable(this.vertexProgram), Optional.ofNullable(this.resultGraph));
                if (!this.features().supportsResultGraphPersistCombination(this.resultGraph, this.persist)) {
                    throw Exceptions.resultGraphPersistCombinationNotSupported(this.resultGraph, this.persist);
                } else if (this.workers > this.features().getMaxWorkers()) {
                    throw Exceptions.computerRequiresMoreWorkersThanSupported(this.workers, this.features().getMaxWorkers());
                }
            }
        }
    }

    protected void loadJars(final Configuration hadoopConfiguration, final Object... params) {
        if (hadoopConfiguration.getBoolean("gremlin.hadoop.jarsInDistributedCache", true)) {
            String hadoopGremlinLibs = null == System.getProperty("HADOOP_GREMLIN_LIBS") ? System.getenv("HADOOP_GREMLIN_LIBS") : System.getProperty("HADOOP_GREMLIN_LIBS");
            if (null == hadoopGremlinLibs) {
                this.logger.warn("HADOOP_GREMLIN_LIBS is not set -- proceeding regardless");
            } else {
                try {
                    Matcher matcher = PATH_PATTERN.matcher(hadoopGremlinLibs);

                    while(true) {
                        while(matcher.find()) {
                            String path = matcher.group();

                            FileSystem fs;
                            try {
                                URI uri = new URI(path);
                                fs = FileSystem.get(uri, hadoopConfiguration);
                            } catch (URISyntaxException var12) {
                                fs = FileSystem.get(hadoopConfiguration);
                            }

                            File file = copyDirectoryIfNonExistent(fs, path);
                            if (file.exists()) {
                                File[] var8 = file.listFiles();
                                int var9 = var8.length;

                                for(int var10 = 0; var10 < var9; ++var10) {
                                    File f = var8[var10];
                                    if (f.getName().endsWith(".jar")) {
                                        this.loadJar(hadoopConfiguration, f, params);
                                    }
                                }
                            } else {
                                this.logger.warn(path + " does not reference a valid directory -- proceeding regardless");
                            }
                        }

                        return;
                    }
                } catch (IOException var13) {
                    throw new IllegalStateException(var13.getMessage(), var13);
                }
            }
        }

    }

    protected abstract void loadJar(final Configuration hadoopConfiguration, final File file, final Object... params) throws IOException;

    public Features features() {
        return new Features();
    }

    public static File copyDirectoryIfNonExistent(final FileSystem fileSystem, final String directory) {
        try {
            String hadoopGremlinLibsRemote = "hadoop-gremlin-" + Gremlin.version() + "-libs";
            Path path = new Path(directory);
            if (Boolean.valueOf(System.getProperty("is.testing", "false")) || fileSystem.exists(path) && fileSystem.isDirectory(path)) {
                File tempDirectory = new File(System.getProperty("java.io.tmpdir"), hadoopGremlinLibsRemote);

                assert tempDirectory.exists() || tempDirectory.mkdirs();

                Path tempPath = new Path(new Path(tempDirectory.toURI()), path.getName());
                RemoteIterator<LocatedFileStatus> files = fileSystem.listFiles(path, false);

                while(files.hasNext()) {
                    LocatedFileStatus f = (LocatedFileStatus)files.next();
                    fileSystem.copyToLocalFile(false, f.getPath(), new Path(tempPath, f.getPath().getName()), true);
                }

                return new File(tempPath.toUri());
            } else {
                return new File(directory);
            }
        } catch (IOException var8) {
            throw new IllegalStateException(var8.getMessage(), var8);
        }
    }

    static {
        PATH_PATTERN = Pattern.compile(File.pathSeparator.equals(":") ? "([^:]|://)+" : "[^" + File.pathSeparator + "]+");
    }

    public class Features implements GraphComputer.Features {
        public Features() {
        }

        public boolean supportsVertexAddition() {
            return false;
        }

        public boolean supportsVertexRemoval() {
            return false;
        }

        public boolean supportsVertexPropertyRemoval() {
            return false;
        }

        public boolean supportsEdgeAddition() {
            return false;
        }

        public boolean supportsEdgeRemoval() {
            return false;
        }

        public boolean supportsEdgePropertyAddition() {
            return false;
        }

        public boolean supportsEdgePropertyRemoval() {
            return false;
        }

        public boolean supportsResultGraphPersistCombination(final GraphComputer.ResultGraph resultGraph, final GraphComputer.Persist persist) {
            if (AbstractHadoopGraphComputer.this.hadoopGraph.configuration().containsKey("gremlin.hadoop.graphWriter")) {
                Object writer = ReflectionUtils.newInstance(AbstractHadoopGraphComputer.this.hadoopGraph.configuration().getGraphWriter(), ConfUtil.makeHadoopConfiguration(AbstractHadoopGraphComputer.this.hadoopGraph.configuration()));
                if (writer instanceof PersistResultGraphAware) {
                    return ((PersistResultGraphAware)writer).supportsResultGraphPersistCombination(resultGraph, persist);
                } else {
                    AbstractHadoopGraphComputer.this.logger.warn(writer.getClass() + " does not implement " + PersistResultGraphAware.class.getSimpleName() + " and thus, persistence options are unknown -- assuming all options are possible");
                    return true;
                }
            } else {
                AbstractHadoopGraphComputer.this.logger.warn("No gremlin.hadoop.graphWriter property provided and thus, persistence options are unknown -- assuming all options are possible");
                return true;
            }
        }

        public boolean supportsDirectObjects() {
            return false;
        }
    }
}
