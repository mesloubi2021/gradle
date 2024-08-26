/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.api.tasks

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import spock.lang.Issue

class DynamicOutputsIncrementalTasksIntegrationTest extends AbstractIntegrationSpec {

    def setup() {
        buildScript '''
            apply plugin: "base"
            
            task generateFiles {
                outputs.files {
                    fileTree("$buildDir/db-scripts").collect { file -> file }
                }
                doLast {
                    file("${buildDir}/db-scripts/").mkdirs()
                    file("${buildDir}/db-scripts/first-script.sql").text = "first"
                    file("${buildDir}/db-scripts/second-script.sql").text = "second"
                }
            }
            
            task copyOutputs(type: Copy) {
                from generateFiles
                into file('dist/db-scripts')
            }'''.stripIndent()
    }

    @Issue("https://github.com/gradle/gradle/issues/2237")
    def "dynamic outputs can be used as input"() {
        when:
        succeeds 'copyOutputs'

        then:
        executedAndNotSkipped ':copyOutputs', ':generateFiles'
    }
}
