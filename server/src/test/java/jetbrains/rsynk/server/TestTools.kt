package jetbrains.rsynk.server

import org.apache.sshd.common.keyprovider.KeyPairProvider
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

object TestTools {
  fun getServerKey(): KeyPairProvider {
    javaClass.classLoader.getResource("private_key.der")
    val privateBytes = javaClass.classLoader.getResource("private_key.der").readBytes()
    val privateKey = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(privateBytes))
    val publicBytes = javaClass.classLoader.getResource("public_key.der").readBytes()
    val publicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(publicBytes))
    return KeyPairProvider { listOf(KeyPair(publicKey, privateKey)) }
  }

  val loremIpsum = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
}

