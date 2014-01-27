package VersionFetcher;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;


/**
 * @goal release
 * @phase process-sources
 */
public class VersionFetcher extends AbstractMojo {

    /**
     * @parameter expression = "${project}"
     * @readonly
     */
    private MavenProject project;

    public void execute() throws MojoExecutionException {
    String version = project.getVersion();
    String release = version;
    if (version.indexOf("-SNAPSHOT") > -1) {
        release = version.substring(0, version.indexOf("-SNAPSHOT"));
        getLog().info("SNAPSHOT found: " + release);
    }
    project.getProperties().setProperty("newVersion", release);
  }
}


