package fr.gaellalire.vestige.resolver.maven;

import java.util.List;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.graph.Dependency;

/**
 *
 * @author Gael Lalire
 */
public class DefaultDependencyCollectionContext implements DependencyCollectionContext {

    private final RepositorySystemSession session;

    private Artifact artifact;

    private Dependency dependency;

    private List<Dependency> managedDependencies;

    public DefaultDependencyCollectionContext(final RepositorySystemSession session, final Artifact artifact, final Dependency dependency,
            final List<Dependency> managedDependencies) {
        this.session = session;
        if (dependency != null) {
            this.artifact = dependency.getArtifact();
        } else {
            this.artifact = artifact;
        }
        this.dependency = dependency;
        this.managedDependencies = managedDependencies;
    }

    public RepositorySystemSession getSession() {
        return session;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public Dependency getDependency() {
        return dependency;
    }

    public List<Dependency> getManagedDependencies() {
        return managedDependencies;
    }

    public void set(final Dependency dependency, final List<Dependency> managedDependencies) {
        artifact = dependency.getArtifact();
        this.dependency = dependency;
        this.managedDependencies = managedDependencies;
    }

    @Override
    public String toString() {
        return String.valueOf(getDependency());
    }

}
