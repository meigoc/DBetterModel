/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */

package meigo.dbettermodel.compat.v2;

import kr.toxicity.model.api.data.renderer.ModelRenderer;
import meigo.dbettermodel.compat.api.BmModel;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

final class V2Model implements BmModel {

    private final ModelRenderer renderer;

    V2Model(ModelRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public String name() {
        return renderer.name();
    }

    @Override
    public Set<String> animations() {
        return renderer.animations().keySet();
    }

    @Override
    public OptionalDouble animationLength(String animation) {
        return renderer.animation(animation)
                .map(anim -> OptionalDouble.of(anim.length()))
                .orElseGet(OptionalDouble::empty);
    }

    @Override
    public Optional<String> animationLoopMode(String animation) {
        return renderer.animation(animation).map(anim -> anim.loop().name());
    }

    @Override
    public Set<String> boneNames() {
        return renderer.rendererGroups().keySet().stream()
                .map(name -> name.name())
                .collect(Collectors.toUnmodifiableSet());
    }
}
