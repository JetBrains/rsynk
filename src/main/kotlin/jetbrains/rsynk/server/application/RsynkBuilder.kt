/**
 * Copyright 2016 - 2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.rsynk.server.application

import org.apache.sshd.common.keyprovider.KeyPairProvider
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

/**
 * This file contains several interfaces with prefix
 * RsynkBuilder_, All of them are used to set required parameters.
 * The chain leads to an interface [RsynkBuilder] with optional
 * parameters which has method [RsynkBuilder.build] returning [Rsynk]
 * */
interface RsynkBuilder_SetPort {
    fun setPort(port: Int): RsynkBuilder_SetWorkerThreads
}

/**
 * Either number of workers or thread executor must be set
 */
interface RsynkBuilder_SetWorkerThreads {
    /**
     * Set number worker threads if you don't want to bother with
     * creating thread executor. This work will be done internally.
     */
    fun setNumberOfWorkerThreads(workersNumber: Int): RsynkBuilder_SetServerKeys

    /**
     * Set your own thread executor if you want to take control
     * over threads used by application
     */
    fun setThreadExecutor(executor: ExecutorService): RsynkBuilder_SetServerKeys
}

interface RsynkBuilder_SetServerKeys {
    fun setRSAKey(privateKey: ByteArray,
                  publicKey: ByteArray): RsynkBuilder
}


/**
 * Last in a chain interface for setting optional
 * parameters and building an [Rsynk] instance
 */
interface RsynkBuilder {
    /**
     * Nio worker threads are used in apache sshd server.
     * Look at [org.apache.sshd.common.FactoryManager].
     * If you don't set the value explicitly - the default
     * value from apache sshd will be used.
     * */
    fun setNumberOfNioWorkers(nioWorkers: Int): RsynkBuilder

    fun setIdleConnectionTimeout(timeout: Long, unit: TimeUnit): RsynkBuilder

    fun build(): Rsynk
}

internal sealed class WorkersThreadPool {
    internal data class DefaultThreadPool(val workersNumber: Int): WorkersThreadPool()
    internal data class CustomThreadPool(val executor: ExecutorService): WorkersThreadPool()
}

internal class Builder internal constructor(private var port: Int?,
                                            private var nioWorkers: Int?,
                                            private var workersNumber: Int?,
                                            private var executor: ExecutorService?,
                                            private var idleConnectionTimeoutMills: Long?,
                                            private var rsaKey: ByteArray?,
                                            private var rsaKeyPub: ByteArray?,
                                            private var maxAuthAttempts: Int?
) : RsynkBuilder_SetPort, RsynkBuilder_SetWorkerThreads, RsynkBuilder_SetServerKeys, RsynkBuilder {

    companion object {
        internal val default: RsynkBuilder_SetPort
            get() = Builder(port = null,
                    nioWorkers = null,
                    workersNumber = null,
                    idleConnectionTimeoutMills = null,
                    executor = null,
                    rsaKey = null,
                    rsaKeyPub = null,
                    maxAuthAttempts = null)
    }

    override fun setNumberOfWorkerThreads(workersNumber: Int): RsynkBuilder_SetServerKeys {
        this.workersNumber = workersNumber
        return this
    }

    override fun setThreadExecutor(executor: ExecutorService): RsynkBuilder_SetServerKeys {
        this.executor = executor
        return this
    }

    override fun setRSAKey(privateKey: ByteArray,
                           publicKey: ByteArray): RsynkBuilder {
        this.rsaKey = privateKey
        this.rsaKeyPub = publicKey
        return this
    }

    override fun setNumberOfNioWorkers(nioWorkers: Int): RsynkBuilder {
        this.nioWorkers = nioWorkers
        return this
    }

    override fun setIdleConnectionTimeout(timeout: Long, unit: TimeUnit): RsynkBuilder {
        this.idleConnectionTimeoutMills = unit.convert(timeout, TimeUnit.MILLISECONDS)
        return this
    }

    override fun setPort(port: Int): RsynkBuilder_SetWorkerThreads {
        this.port = port
        return this
    }

    override fun build(): Rsynk {
        // it's safe to call !! operator here
        // properties are guaranteed to be
        // initialized by interfaces design
        val port = this.port!!
        val nioWorkers = this.nioWorkers
        val workersNumber = this.workersNumber
        val executor = this.executor
        val workerThreadPool = if (workersNumber != null) {
            WorkersThreadPool.DefaultThreadPool(workersNumber)
        } else {
            WorkersThreadPool.CustomThreadPool(executor!!)
        }
        val idleConnectionTimeout = this.idleConnectionTimeoutMills
        val serverKeysProvider = readServerKeys(this.rsaKey!!, this.rsaKeyPub!!)

        return Rsynk(port, nioWorkers, workerThreadPool, idleConnectionTimeout, serverKeysProvider)
    }

    private fun readServerKeys(private: ByteArray,
                               public: ByteArray): KeyPairProvider {
        val privateKey = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(private))
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(public))
        return KeyPairProvider { listOf(KeyPair(publicKey, privateKey)) }
    }
}
