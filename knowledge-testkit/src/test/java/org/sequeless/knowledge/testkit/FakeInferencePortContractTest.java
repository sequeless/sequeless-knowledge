package org.sequeless.knowledge.testkit;

import org.sequeless.knowledge.spi.InferencePort;

/** Runs the {@link InferencePortContract} against {@link FakeInferencePort}. */
class FakeInferencePortContractTest extends InferencePortContract {

    @Override
    protected InferencePort newPort() {
        return FakeInferencePort.replying("reply");
    }
}
