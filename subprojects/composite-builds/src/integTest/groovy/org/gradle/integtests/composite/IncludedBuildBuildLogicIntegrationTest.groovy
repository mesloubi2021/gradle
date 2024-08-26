/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.integtests.composite

import org.gradle.integtests.fixtures.ToBeFixedForInstantExecution

class IncludedBuildBuildLogicIntegrationTest extends AbstractCompositeBuildIntegrationTest {
    def setup() {
        buildA.buildFile.text = """
            plugins {
                id 'org.test.plugin.pluginBuild'
            }
        """ + buildA.buildFile.text
    }

    @ToBeFixedForInstantExecution
    def "using Java-based plugin from included build does not leak Gradle API"() {
        given:
        def pluginBuild = pluginProjectBuild("pluginBuild")
        includeBuild pluginBuild
        buildA.buildFile << """
            import ${com.google.common.collect.ImmutableList.canonicalName}
        """

        when:
        fails(buildA, "taskFromPluginBuild")

        then:
        failure.assertHasErrorOutput("unable to resolve class com.google.common.collect.ImmutableList")
    }

    @ToBeFixedForInstantExecution
    def "using precompiled Groovy plugin from included build does not leak Gradle API"() {
        given:
        def pluginBuild = singleProjectBuild("pluginBuild")
        includeBuild pluginBuild
        pluginBuild.buildFile << """
            apply plugin: 'groovy-gradle-plugin'
            
            group = 'org.test.plugin'
        """
        pluginBuild.file("src/main/groovy/org.test.plugin.pluginBuild.gradle") << """
            task taskFromPluginBuild {
                group = "Plugin"
            }
        """
        buildA.buildFile << """
            import ${com.google.common.collect.ImmutableList.canonicalName}
        """

        when:
        fails(buildA, "assemble")

        then:
        failure.assertHasErrorOutput("unable to resolve class com.google.common.collect.ImmutableList")
    }
}
