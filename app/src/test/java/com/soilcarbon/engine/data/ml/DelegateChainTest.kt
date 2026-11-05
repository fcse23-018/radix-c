package com.soilcarbon.engine.data.ml

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DelegateChainTest {

    @Test
    fun `GPU preference tries GPU then NNAPI then CPU`() {
        assertThat(delegateChain(preferGpu = true)).containsExactly(
            DelegateKind.GPU,
            DelegateKind.NNAPI,
            DelegateKind.CPU,
        ).inOrder()
    }

    @Test
    fun `disabling GPU skips straight to NNAPI then CPU`() {
        assertThat(delegateChain(preferGpu = false)).containsExactly(
            DelegateKind.NNAPI,
            DelegateKind.CPU,
        ).inOrder()
    }
}
