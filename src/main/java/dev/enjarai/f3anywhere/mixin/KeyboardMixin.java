package dev.enjarai.f3anywhere.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.Keyboard;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
	@Shadow protected abstract boolean processF3(int key);

	@Inject(
			method = "onKey",
			slice = @Slice(
					from = @At(
							value = "CONSTANT",
							args = "intValue=66"
					)
			),
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/screen/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V",
					ordinal = 0
			)
	)
	private void processDebugKeysEarly(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci,
									   @Share("f3Result") LocalRef<Boolean> f3Result) {
		if (
				// Action 0 is pressing a button, 1 is releasing. We're only interested in presses.
				action != 0
				// F3 must also already be pressed,
				// we need to do these checks ourselves since we're bypassing the vanilla logic for them.
				&& InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_F3)
				// If the key code is smaller than F1, it's part of the range [a-b,0-9],
				// this means we definitely want to handle it before screens do.
				&& key < InputUtil.GLFW_KEY_F1
		) {
			f3Result.set(processF3(key));
		}
	}

	@WrapOperation(
			method = "onKey",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/Keyboard;processF3(I)Z"
			)
	)
	private boolean returnResultOfEarlyDebugKeys(Keyboard instance, int key, Operation<Boolean> original, @Share("f3Result") LocalRef<Boolean> f3Result) {
		// If we've already executed the debug key logic earlier, we wouldn't want to do it again.
		// Luckily we've saved the return value in advance, so we can give it back here.
		return f3Result.get() == null ? original.call(instance, key) : f3Result.get();
	}
}