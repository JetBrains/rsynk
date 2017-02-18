package jetbrains.rsynk

import jetbrains.rsynk.protocol.CompatFlags
import jetbrains.rsynk.protocol.decode
import jetbrains.rsynk.protocol.encode
import org.junit.Assert
import org.junit.Test

class CompatFlagsTest {
  @Test
  fun all_values_are_powers_of_two_test() {
    getAllFlags().forEach { flag ->
      Assert.assertTrue(flag.value.isPowerOfTwo())
    }
  }

  @Test
  fun have_all_required_powers_of_two_test() {
    val availablePowersOfTwo: Set<Int> = getAllFlags().map { it.value }.toSet()
    arrayOf(1, 2, 4, 8, 16, 32).forEach { value ->
      Assert.assertTrue("$value is not found among compat-flags", availablePowersOfTwo.contains(value))
    }
  }

  @Test
  fun encode_empty_set_to_zero_test() {
    Assert.assertEquals(0.toByte(), emptySet<CompatFlags>().encode())
  }

  @Test
  fun encode_full_values_to_63_test() {
    Assert.assertEquals(63.toByte(), getAllFlags().encode())
  }

  @Test
  fun decode_zero_to_empty_set_test() {
    Assert.assertEquals(emptySet<CompatFlags>(), 0.toByte().decode())
  }

  @Test
  fun decode_63_to_full_values_set_test() {
    Assert.assertEquals(getAllFlags(), 63.toByte().decode())
  }

  private fun getAllFlags(): Set<CompatFlags> {
    val allFlags = setOf(CompatFlags.CF_INC_RECURSE,
            CompatFlags.CF_SYMLINK_TIMES,
            CompatFlags.CF_SYMLINK_ICONV,
            CompatFlags.CF_SAFE_FLIST,
            CompatFlags.CF_AVOID_XATTR_OPTIM,
            CompatFlags.CF_CHKSUM_SEED_FIX)
    val allFlagsText = allFlags.map { flag -> flag.javaClass.name }
    CompatFlags::class.nestedClasses.forEach { nestedClass ->
      Assert.assertTrue("Did you added new compat-flag? You have add that to tests!",
              allFlagsText.contains(nestedClass.java.name))
    }
    return allFlags
  }

  private fun Int.isPowerOfTwo(): Boolean = this.and(this - 1) == 0
}