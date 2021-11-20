/*
 * This file is part of Vestige.
 *
 * Vestige is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Vestige is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Vestige.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.gaellalire.vestige.application.descriptor.xml;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import fr.gaellalire.vestige.application.descriptor.xml.schema.application.ActivateNamedModules;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.AddExports;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.AddOpens;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.AddReads;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Attachment;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Inject;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.MavenClassType;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Mode;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModifyLoadedDependency;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModifyScope;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModulePackageName;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.Signatures;
import fr.gaellalire.vestige.application.descriptor.xml.schema.application.URLsClassType;
import fr.gaellalire.vestige.application.manager.AddInject;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.ApplicationResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.application.manager.AttachmentDescriptor;
import fr.gaellalire.vestige.application.manager.PermissionSetFactory;
import fr.gaellalire.vestige.application.manager.VerificationMetadata;
import fr.gaellalire.vestige.spi.job.JobHelper;
import fr.gaellalire.vestige.spi.resolver.ResolverException;
import fr.gaellalire.vestige.spi.resolver.Scope;
import fr.gaellalire.vestige.spi.resolver.maven.CreateClassLoaderConfigurationRequest;
import fr.gaellalire.vestige.spi.resolver.maven.MavenContext;
import fr.gaellalire.vestige.spi.resolver.maven.ModifyLoadedDependencyRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ResolveMavenArtifactRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ResolveMode;
import fr.gaellalire.vestige.spi.resolver.maven.ResolvedMavenArtifact;
import fr.gaellalire.vestige.spi.resolver.url_list.URLListRequest;
import fr.gaellalire.vestige.utils.AnyURIProperty;
import fr.gaellalire.vestige.utils.SimpleValueGetter;

/**
 * @author Gael Lalire
 */
public class XMLAttachmentDescriptor implements AttachmentDescriptor {

    private Attachment attachment;

    private XMLApplicationRepositoryManager xmlApplicationRepositoryManager;

    private JobHelper jobHelper;

    private MavenContext mavenContext;

    private PermissionSetFactory permissions;

    public XMLAttachmentDescriptor(final Attachment attachment, final XMLApplicationRepositoryManager xmlApplicationRepositoryManager, final JobHelper jobHelper,
            final MavenContext mavenContext, final PermissionSetFactory permissions) {
        this.attachment = attachment;
        this.xmlApplicationRepositoryManager = xmlApplicationRepositoryManager;
        this.jobHelper = jobHelper;
        this.mavenContext = mavenContext;
        this.permissions = permissions;
    }

    public static Scope convertScope(final fr.gaellalire.vestige.application.descriptor.xml.schema.application.Scope scope) throws ApplicationException {
        Scope mavenScope;
        switch (scope) {
        case ATTACHMENT:
            mavenScope = Scope.ATTACHMENT;
            break;
        case INSTALLATION:
            mavenScope = Scope.CLASS_LOADER_CONFIGURATION;
            break;
        case PLATFORM:
            mavenScope = Scope.PLATFORM;
            break;
        default:
            throw new ApplicationException("Unknown scope " + scope);
        }
        return mavenScope;
    }

    public ApplicationResolvedClassLoaderConfiguration resolve(final MavenContext mavenContext, final String configurationName, final MavenClassType mavenClassType,
            final JobHelper actionHelper) throws ApplicationException {
        ResolveMode resolveMode;
        Mode mode = mavenClassType.getMode();
        switch (mode) {
        case CLASSPATH:
            resolveMode = ResolveMode.CLASSPATH;
            break;
        case FIXED_DEPENDENCIES:
            resolveMode = ResolveMode.FIXED_DEPENDENCIES;
            break;
        default:
            throw new ApplicationException("Unknown launch mode " + mode);
        }

        ResolveMavenArtifactRequest resolveMavenArtifactRequest = mavenContext.resolve(SimpleValueGetter.INSTANCE.getValue(mavenClassType.getGroupId()),
                SimpleValueGetter.INSTANCE.getValue(mavenClassType.getArtifactId()), SimpleValueGetter.INSTANCE.getValue(mavenClassType.getVersion()));

        ResolvedMavenArtifact resolvedMavenArtifact;
        try {
            resolvedMavenArtifact = resolveMavenArtifactRequest.execute(actionHelper);
        } catch (ResolverException e) {
            throw new ApplicationException(e);
        }

        CreateClassLoaderConfigurationRequest createClassLoaderConfigurationRequest = resolvedMavenArtifact.createClassLoaderConfiguration(configurationName, resolveMode,
                convertScope(mavenClassType.getScope()));

        for (ModifyScope modifyScope : mavenClassType.getModifyScope()) {
            createClassLoaderConfigurationRequest.addModifyScope(SimpleValueGetter.INSTANCE.getValue(modifyScope.getGroupId()),
                    SimpleValueGetter.INSTANCE.getValue(modifyScope.getArtifactId()), convertScope(modifyScope.getScope()));
        }

        List<ModifyLoadedDependency> modifyLoadedDependencyList = mavenClassType.getModifyLoadedDependency();
        if (modifyLoadedDependencyList != null) {
            for (ModifyLoadedDependency modifyDependency : modifyLoadedDependencyList) {
                ModifyLoadedDependencyRequest modifyDependencyRequest = createClassLoaderConfigurationRequest.addModifyLoadedDependency(
                        SimpleValueGetter.INSTANCE.getValue(modifyDependency.getGroupId()), SimpleValueGetter.INSTANCE.getValue(modifyDependency.getArtifactId()),
                        SimpleValueGetter.INSTANCE.getValue(modifyDependency.getClassifier()));
                for (ModulePackageName addExports : modifyDependency.getAddExports()) {
                    modifyDependencyRequest.addExports(SimpleValueGetter.INSTANCE.getValue(addExports.getModule()), SimpleValueGetter.INSTANCE.getValue(addExports.getPackage()));
                }
                for (ModulePackageName addExports : modifyDependency.getAddOpens()) {
                    modifyDependencyRequest.addOpens(SimpleValueGetter.INSTANCE.getValue(addExports.getModule()), SimpleValueGetter.INSTANCE.getValue(addExports.getPackage()));
                }
                if (modifyDependency.getAddBeforeParent() != null) {
                    modifyDependencyRequest.setBeforeParent(true);
                }
                modifyDependencyRequest.execute();
            }
        }

        ActivateNamedModules activateNamedModules = mavenClassType.getActivateNamedModules();
        if (activateNamedModules != null) {
            createClassLoaderConfigurationRequest.setNamedModuleActivated(true);
            for (AddReads addReads : activateNamedModules.getAddReads()) {
                createClassLoaderConfigurationRequest.addReads(SimpleValueGetter.INSTANCE.getValue(addReads.getSource()),
                        SimpleValueGetter.INSTANCE.getValue(addReads.getTarget()));
            }
            for (AddExports addExports : activateNamedModules.getAddExports()) {
                createClassLoaderConfigurationRequest.addExports(SimpleValueGetter.INSTANCE.getValue(addExports.getSource()),
                        SimpleValueGetter.INSTANCE.getValue(addExports.getPn()), SimpleValueGetter.INSTANCE.getValue(addExports.getTarget()));
            }
            for (AddOpens addOpens : activateNamedModules.getAddOpens()) {
                createClassLoaderConfigurationRequest.addOpens(SimpleValueGetter.INSTANCE.getValue(addOpens.getSource()), SimpleValueGetter.INSTANCE.getValue(addOpens.getPn()),
                        SimpleValueGetter.INSTANCE.getValue(addOpens.getTarget()));
            }
        }

        try {
            return new ApplicationResolvedClassLoaderConfiguration(createClassLoaderConfigurationRequest.execute(actionHelper),
                    xmlApplicationRepositoryManager.getVestigeMavenResolverIndex());
        } catch (ResolverException e) {
            throw new ApplicationException(e);
        }

    }

    public ApplicationResolvedClassLoaderConfiguration getClassLoaderConfiguration(final String configurationName) throws ApplicationException {
        URLsClassType urlsInstaller = attachment.getUrlListResolver();
        if (urlsInstaller != null) {
            URLListRequest urlListRequest = xmlApplicationRepositoryManager.getVestigeURLListResolver().createURLListRequest(convertScope(urlsInstaller.getScope()),
                    configurationName);

            for (AnyURIProperty string : urlsInstaller.getUrl()) {
                try {
                    urlListRequest.addURL(new URL(SimpleValueGetter.INSTANCE.getValue(string)));
                } catch (MalformedURLException e) {
                    throw new ApplicationException("Not an URL", e);
                }
            }

            for (fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModulePackageName modulePackageName : urlsInstaller.getAddExports()) {
                urlListRequest.addExports(SimpleValueGetter.INSTANCE.getValue(modulePackageName.getModule()), SimpleValueGetter.INSTANCE.getValue(modulePackageName.getPackage()));
            }
            for (fr.gaellalire.vestige.application.descriptor.xml.schema.application.ModulePackageName modulePackageName : urlsInstaller.getAddOpens()) {
                urlListRequest.addOpens(SimpleValueGetter.INSTANCE.getValue(modulePackageName.getModule()), SimpleValueGetter.INSTANCE.getValue(modulePackageName.getPackage()));
            }

            try {
                return new ApplicationResolvedClassLoaderConfiguration(urlListRequest.execute(jobHelper), xmlApplicationRepositoryManager.getVestigeURLListResolverIndex());
            } catch (ResolverException e) {
                throw new ApplicationException(e);
            }
        }
        return resolve(mavenContext, configurationName, attachment.getMavenResolver(), jobHelper);
    }

    @Override
    public PermissionSetFactory getPermissions() throws ApplicationException {
        return permissions;
    }

    @Override
    public List<AddInject> getAddInjects() throws ApplicationException {
        List<AddInject> addInjects = new ArrayList<AddInject>();
        for (Inject inject : attachment.getInject()) {
            addInjects.add(new AddInject(SimpleValueGetter.INSTANCE.getValue(inject.getServiceClassName()), SimpleValueGetter.INSTANCE.getValue(inject.getTargetServiceClassName()),
                    SimpleValueGetter.INSTANCE.getValue(inject.getSetterName())));
        }
        return addInjects;
    }

    @Override
    public VerificationMetadata getVerificationMetadata() throws ApplicationException {
        fr.gaellalire.vestige.application.descriptor.xml.schema.application.VerificationMetadata verificationMetadata = attachment.getVerificationMetadata();
        if (verificationMetadata == null) {
            return null;
        }
        Signatures signatures = verificationMetadata.getSignatures();
        String pgpSignature = null;
        if (signatures != null) {
            pgpSignature = SimpleValueGetter.INSTANCE.getValue(signatures.getPgpSignature());
        }
        return new VerificationMetadata(SimpleValueGetter.INSTANCE.getValue(verificationMetadata.getText()), pgpSignature);
    }

}
