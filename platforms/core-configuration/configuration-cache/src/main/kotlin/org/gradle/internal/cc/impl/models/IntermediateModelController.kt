/*
 * Copyright 2021 the original author or authors.
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

package org.gradle.internal.cc.impl.models

import org.gradle.internal.cc.base.serialize.HostServiceProvider
import org.gradle.internal.cc.base.serialize.IsolateOwners
import org.gradle.internal.cc.impl.ConfigurationCacheStateStore
import org.gradle.internal.cc.impl.ConfigurationCacheUserTypesIO
import org.gradle.internal.cc.impl.StateType
import org.gradle.internal.cc.impl.cacheentry.ModelKey
import org.gradle.internal.cc.impl.fingerprint.ConfigurationCacheFingerprintController
import org.gradle.internal.model.CalculatedValueContainerFactory
import org.gradle.internal.serialize.Decoder
import org.gradle.internal.serialize.Encoder
import org.gradle.internal.serialize.graph.readNonNull
import org.gradle.tooling.provider.model.UnknownModelException
import org.gradle.tooling.provider.model.internal.ToolingModelParameterCarrier
import org.gradle.util.Path


/**
 * Responsible for loading and storing intermediate models used during tooling API build action execution.
 */
internal
class IntermediateModelController(
    private val host: HostServiceProvider,
    private val cacheIO: ConfigurationCacheUserTypesIO,
    store: ConfigurationCacheStateStore,
    calculatedValueContainerFactory: CalculatedValueContainerFactory,
    private val cacheFingerprintController: ConfigurationCacheFingerprintController
) : ProjectStateStore<ModelKey, IntermediateModel>(store, StateType.IntermediateModels, "intermediate model", calculatedValueContainerFactory) {
    override fun projectPathForKey(key: ModelKey) = key.identityPath

    override fun write(encoder: Encoder, value: IntermediateModel) {
        cacheIO.writeWithUserTypes(encoder, IsolateOwners.OwnerHost(host)) {
            write(value)
        }
    }

    override fun read(decoder: Decoder): IntermediateModel {
        return cacheIO.readWithUserTypes(decoder, IsolateOwners.OwnerHost(host)) {
            readNonNull()
        }
    }

    fun <T> loadOrCreateIntermediateModel(identityPath: Path?, modelName: String, parameter: ToolingModelParameterCarrier?, creator: () -> T): T? {
        val key = ModelKey(identityPath, modelName, parameter?.hash)
        return loadOrCreateValue(key) {
            try {
                val model = if (identityPath != null) {
                    cacheFingerprintController.runCollectingFingerprintForProject(identityPath, creator)
                } else {
                    creator()
                }
                if (model == null) IntermediateModel.NullModel else IntermediateModel.Model(model)
            } catch (e: UnknownModelException) {
                IntermediateModel.NoModel(e.message!!)
            }
        }.result()
    }
}
