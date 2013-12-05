/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.ide.visualstudio;

import org.gradle.api.Incubating;
import org.gradle.api.Named;
import org.gradle.language.base.BuildableModelElement;
import org.gradle.nativebinaries.NativeComponent;

import java.util.Set;

/**
 * A visual studio solution, representing one or more {@link org.gradle.nativebinaries.NativeBinary} instances
 * from the same {@link NativeComponent}.
 */
@Incubating
public interface VisualStudioSolution extends Named, BuildableModelElement {
    /**
     * The set of projects included in this solution.
     */
    Set<VisualStudioProject> getProjects();

    /**
     * The component that this solution represents.
     */
    NativeComponent getComponent();

    /**
     * Configuration for the generated solution file.
     */
    TextConfigFile getSolutionFile();
}
