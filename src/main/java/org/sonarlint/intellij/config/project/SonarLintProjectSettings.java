/*
 * SonarLint for IntelliJ IDEA
 * Copyright (C) 2015-2020 SonarSource
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.intellij.config.project;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.util.xmlb.XmlSerializerUtil;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

@State(name = "SonarLintProjectSettings", storages = {@Storage("sonarlint.xml")})
public final class SonarLintProjectSettings implements PersistentStateComponent<SonarLintProjectSettings> {
  private static final Logger LOGGER = Logger.getInstance(SonarLintProjectSettings.class);

  private boolean verboseEnabled = false;
  private boolean analysisLogsEnabled = false;
  private Map<String, String> additionalProperties = new LinkedHashMap<>();
  private boolean bindingEnabled = false;
  private String serverId = null;
  private List<String> fileExclusions = new ArrayList<>();
  private Map<String, String> vcsRootMapping = new LinkedHashMap<>();

  /**
   * Constructor called by the XML serialization and deserialization (no args).
   * Even though this class has the scope of a project, we can't have it injected here.
   */
  public SonarLintProjectSettings() {

  }

  /**
   * TODO Replace @Deprecated with @NonInjectable when switching to 2019.3 API level
   * @deprecated in 4.2 to silence a check in 2019.3
   */
  @Deprecated
  public SonarLintProjectSettings(SonarLintProjectSettings toCopy) {
    XmlSerializerUtil.copyBean(toCopy, this);
  }

  @Override
  public synchronized SonarLintProjectSettings getState() {
    return this;
  }

  @Override
  public synchronized void loadState(SonarLintProjectSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public boolean isVerboseEnabled() {
    return verboseEnabled;
  }

  public void setVerboseEnabled(boolean verboseEnabled) {
    this.verboseEnabled = verboseEnabled;
  }

  public Map<String, String> getAdditionalProperties() {
    return additionalProperties;
  }

  public void setAdditionalProperties(Map<String, String> additionalProperties) {
    this.additionalProperties = new LinkedHashMap<>(additionalProperties);
  }

  @CheckForNull
  public String getServerId() {
    return serverId;
  }

  public void setServerId(@Nullable String serverId) {
    this.serverId = serverId;
  }

  public boolean isBindingEnabled() {
    return bindingEnabled;
  }

  public void setBindingEnabled(boolean bindingEnabled) {
    this.bindingEnabled = bindingEnabled;
  }

  public boolean isAnalysisLogsEnabled() {
    return analysisLogsEnabled;
  }

  public void setAnalysisLogsEnabled(boolean analysisLogsEnabled) {
    this.analysisLogsEnabled = analysisLogsEnabled;
  }

  public List<String> getFileExclusions() {
    return new ArrayList<>(fileExclusions);
  }

  public void setFileExclusions(List<String> fileExclusions) {
    this.fileExclusions = new ArrayList<>(fileExclusions);
  }

  public Map<String, String> getVcsRootMapping() {
    return vcsRootMapping;
  }

  public void setVcsRootMapping(Map<String, String> vcsRootMapping) {
    this.vcsRootMapping = new LinkedHashMap<>(vcsRootMapping);
  }

  public static String resolveProjectkey(Project project, Module module, SonarLintProjectSettings projectSettings) {
    if (ApplicationManager.getApplication().isDispatchThread()) {
      try {
        final FutureTask<String> futureTask = new FutureTask<>(() ->
                resolveKey(project, module, projectSettings));
        ApplicationManager.getApplication().executeOnPooledThread(futureTask);
        return futureTask.get();
      } catch (InterruptedException | ExecutionException e) {
        LOGGER.error(e.getMessage(), e);
        return null;
      }
    } else {
      return resolveKey(project, module, projectSettings);
    }
  }

  @org.jetbrains.annotations.Nullable
  private static String resolveKey(Project project, Module module, SonarLintProjectSettings projectSettings) {
    if (projectSettings.getVcsRootMapping().isEmpty()) {
      LOGGER.debug("No vcs root mappings");
      return null;
    } else {
      return Optional.ofNullable(module).map(m ->
              Optional.ofNullable(m.getModuleFile())
                      .orElseGet(() -> Arrays.stream(ModuleRootManager.getInstance(m).getContentRoots()).findFirst().orElse(null)))
              .map(virtualFile -> ProjectLevelVcsManager.getInstance(project).getVcsRootFor(virtualFile))
              .flatMap(root -> Optional.ofNullable(projectSettings.getVcsRootMapping().get(root.getCanonicalPath())))
              .orElseGet(() -> {
                LOGGER.info("No project key found for " + module.getName());
                return null;
              });
    }
  }

}
