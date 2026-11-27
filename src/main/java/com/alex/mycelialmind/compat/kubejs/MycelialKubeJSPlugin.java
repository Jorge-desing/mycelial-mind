package com.alex.mycelialmind.compat.kubejs;

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

/**
 * Optional KubeJS plugin discovered via {@code kubejs.plugins.txt}.
 * Exposes {@link MycelialKubeJSBindings} as the {@code MycelialMind} script global.
 */
public final class MycelialKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("MycelialMind", MycelialKubeJSBindings.class);
    }
}
