/*
 * Copyright 2021 Jeremy KUHN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.winterframework.tools.maven.internal.task;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ClassifierFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.filter.collection.GroupIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.apache.maven.shared.artifact.filter.collection.TypeFilter;
import org.codehaus.plexus.util.StringUtils;

import io.winterframework.tools.maven.internal.DependencyModule;
import io.winterframework.tools.maven.internal.Task;
import io.winterframework.tools.maven.internal.TaskExecutionException;

/**
 * @author jkuhn
 *
 */
public class ResolveDependenciesTask extends Task<Set<DependencyModule>> {

	private final Set<Artifact> artifacts;
	private final Optional<Path> projectJModsOverridePath;
	private final Path jmodsExplodedPath;
	private final Path jmodsPath;

	private boolean overWriteIfNewer;
	
    private String includeScope;
    private String excludeScope;
    
    private String includeTypes;
    private String excludeTypes;
    
    private String includeClassifiers;
    private String excludeClassifiers;
	
    private String excludeArtifactIds;
    private String includeArtifactIds;

    private String includeGroupIds;
    private String excludeGroupIds;
	
	public ResolveDependenciesTask(AbstractMojo mojo, Set<Artifact> artifacts, Optional<Path> projectJModsOverridePath, Path jmodsExplodedPath, Path jmodsPath) {
		super(mojo);
		this.artifacts = artifacts;
		this.projectJModsOverridePath = projectJModsOverridePath;
		this.jmodsExplodedPath = jmodsExplodedPath;
		this.jmodsPath = jmodsPath;
	}

	@Override
	public Set<DependencyModule> call() throws TaskExecutionException {
		if(this.verbose) {
			this.getLog().info("[ Resolving project dependencies... ]");
		}
		try {
			// add filters in well known order, least specific to most specific
			FilterArtifacts filter = new FilterArtifacts();
			filter.addFilter(new ScopeFilter(cleanToBeTokenizedString(this.includeScope), cleanToBeTokenizedString(this.excludeScope)));
			filter.addFilter(new TypeFilter(cleanToBeTokenizedString(this.includeTypes), cleanToBeTokenizedString(this.excludeTypes)));
			filter.addFilter(new ClassifierFilter(cleanToBeTokenizedString(this.includeClassifiers), cleanToBeTokenizedString(this.excludeClassifiers)));
			filter.addFilter(new GroupIdFilter(cleanToBeTokenizedString(this.includeGroupIds), cleanToBeTokenizedString(this.excludeGroupIds)));
			filter.addFilter(new ArtifactIdFilter(cleanToBeTokenizedString(this.includeArtifactIds), cleanToBeTokenizedString(this.excludeArtifactIds)));

			// Filter artifacts
			Set<Artifact> filteredArtifacts = filter.filter(this.artifacts);
			
			ModuleFinder moduleFinder = ModuleFinder.of(filteredArtifacts.stream().map(artifact -> artifact.getFile().toPath()).toArray(Path[]::new));
			Map<Path, ModuleReference> filteredModules = moduleFinder.findAll().stream().collect(Collectors.toMap(moduleRef -> Paths.get(moduleRef.location().get()), Function.identity()));

			Set<DependencyModule> dependencies = new HashSet<>();
			for(Artifact artifact : filteredArtifacts) {
				dependencies.add(new DependencyModule(artifact, filteredModules.get(artifact.getFile().toPath()), this.projectJModsOverridePath, this.jmodsExplodedPath, this.jmodsPath, this.overWriteIfNewer));
			}
			
			if(this.verbose) {
				for(DependencyModule dependency : dependencies) {
					this.getLog().info(" - " + dependency);
				}
			}
			return dependencies;
		}
		catch (ArtifactFilterException | IOException e) {
			throw new TaskExecutionException("Error resolving project dependencies", e);
		}
	}
	
	private static String cleanToBeTokenizedString(String str) {
		String ret = "";
		if (!StringUtils.isEmpty(str)) {
			// remove initial and ending spaces, plus all spaces next to commas
			ret = str.trim().replaceAll("[\\s]*,[\\s]*", ",");
		}
		return ret;
	}

	public boolean isOverWriteIfNewer() {
		return overWriteIfNewer;
	}

	public void setOverWriteIfNewer(boolean overWriteIfNewer) {
		this.overWriteIfNewer = overWriteIfNewer;
	}

	public String getIncludeScope() {
		return includeScope;
	}

	public void setIncludeScope(String includeScope) {
		this.includeScope = includeScope;
	}

	public String getExcludeScope() {
		return excludeScope;
	}

	public void setExcludeScope(String excludeScope) {
		this.excludeScope = excludeScope;
	}

	public String getIncludeTypes() {
		return includeTypes;
	}

	public void setIncludeTypes(String includeTypes) {
		this.includeTypes = includeTypes;
	}

	public String getExcludeTypes() {
		return excludeTypes;
	}

	public void setExcludeTypes(String excludeTypes) {
		this.excludeTypes = excludeTypes;
	}

	public String getIncludeClassifiers() {
		return includeClassifiers;
	}

	public void setIncludeClassifiers(String includeClassifiers) {
		this.includeClassifiers = includeClassifiers;
	}

	public String getExcludeClassifiers() {
		return excludeClassifiers;
	}

	public void setExcludeClassifiers(String excludeClassifiers) {
		this.excludeClassifiers = excludeClassifiers;
	}

	public String getExcludeArtifactIds() {
		return excludeArtifactIds;
	}

	public void setExcludeArtifactIds(String excludeArtifactIds) {
		this.excludeArtifactIds = excludeArtifactIds;
	}

	public String getIncludeArtifactIds() {
		return includeArtifactIds;
	}

	public void setIncludeArtifactIds(String includeArtifactIds) {
		this.includeArtifactIds = includeArtifactIds;
	}

	public String getIncludeGroupIds() {
		return includeGroupIds;
	}

	public void setIncludeGroupIds(String includeGroupIds) {
		this.includeGroupIds = includeGroupIds;
	}

	public String getExcludeGroupIds() {
		return excludeGroupIds;
	}

	public void setExcludeGroupIds(String excludeGroupIds) {
		this.excludeGroupIds = excludeGroupIds;
	}
}
