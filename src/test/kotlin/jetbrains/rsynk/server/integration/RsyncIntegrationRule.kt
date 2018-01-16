package jetbrains.rsynk.server.integration

import org.junit.Assert
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement


/**
 * Rule to use in rsync client integration tests.
 * It makes a test suite fail before any test
 * executed if rsync client cannot be found
 * under expected location.
 */
class RsyncIntegrationRule : TestRule {
    override fun apply(base: Statement, description: Description?) = object : Statement() {
        override fun evaluate() {
            Assert.assertTrue("rsync executable is not found", Rsync.isInstalled())
            base.evaluate()
        }
    }
}
