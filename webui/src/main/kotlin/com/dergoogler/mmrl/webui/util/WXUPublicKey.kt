@file:Suppress("FunctionName")

package com.dergoogler.mmrl.webui.util

import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
val WXUPublicKey
    get(): PublicKey {
        val publicKeyPEM =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkT7SQr9BKze1uuZxZjVFosWrVfJw0g3ulgt/N8TqYPaYXchkaKU3l8MubAOgVrYRfAcWPxFaZpeGbw5MRQkPwBqgXut8uUTI4/YfhDO6HBn4k9tFIMYTJiIJHTRJ+dXFHbfj8IKr53LQGs40rwfD83DZvEUxT6Cn7/a77oMVSMPSP9TDULRK8tvnmWJJbQACHz/bHxYkqo3DZNre09GHFOZiD3fxoaWBwGO3a0wUIkNkHpoSSX0itmcdPCXH0Sq8y/nWP6MSjTBzstTEJgMW1/Ej8FBnjvjCSZvWLmG3aVfoulig5o+rywKxLYsKLO4xnZb2+mCmHFa/zYOglNnQtwIDAQAB"
        val encoded: ByteArray = Base64.decode(publicKeyPEM)
        val keySpec = X509EncodedKeySpec(encoded)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec)
    }