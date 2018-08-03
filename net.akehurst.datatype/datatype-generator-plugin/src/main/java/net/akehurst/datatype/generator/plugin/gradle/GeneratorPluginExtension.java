/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.datatype.generator.plugin.gradle;

import java.io.File;
import java.util.List;

import org.gradle.api.Project;

public class GeneratorPluginExtension {

    public static final String NAME = "generate";

    Project project;

    public File templateFile;
    public File outputFile;
    public List<String> classPatterns;

    public GeneratorPluginExtension(final Project project) {// , final GeneratorPluginConvention convention) {
        this.project = project;

    }

    public static GeneratorPluginExtension get(final Project project) {
        return project.getExtensions().getByType(GeneratorPluginExtension.class);
    }

}