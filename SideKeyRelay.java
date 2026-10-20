package net.automods.sidekeyrelay;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * SideKeyRelay（侧键中继）—— Forge 1.12.2 客户端模组。
 *
 * 解决的问题：绑定到鼠标侧键（button &gt; 2）的快捷键，需要先按下其他按键才能正常工作。
 * 原因：FML 1.12.2 只在键盘事件循环（Minecraft#runTickKeyboard 的 while (Keyboard.next())）
 * 里通过 FMLCommonHandler#fireKeyInput 发布 InputEvent.KeyInputEvent。按下侧键时
 * KeyBinding 的状态虽然已被更新（Minecraft#runTickMouse 会调用 setKeyBindState），
 * 但只在 KeyInputEvent 回调中轮询快捷键的模组（如 AE2 终端类模组）感知不到这次按压；
 * 直到按了任意键盘键，KeyInputEvent 才补发一次，侧键快捷键这才生效。
 *
 * 修复方式：侧键事件（按下与松开都算）处理完毕后补发一个 KeyInputEvent，
 * 唤醒这些轮询型监听器。1.12.2 上 KeyBinding#isPressed() 依赖 onTick 累加的
 * pressTime，而 MouseEvent 发布在 onTick 之前，所以补发时机选在其后发布的
 * InputEvent.MouseInputEvent（详见 ClientEventHandler）。
 */
@Mod(
    modid = SideKeyRelay.MODID,
    name = SideKeyRelay.NAME,
    version = SideKeyRelay.VERSION,
    acceptedMinecraftVersions = "[1.12.2]")
public final class SideKeyRelay {

    public static final String MODID = "sidekeyrelay";
    public static final String NAME = "SideKeyRelay";
    public static final String VERSION = "1.1.0";

    @SidedProxy(
        clientSide = "net.automods.sidekeyrelay.ClientProxy",
        serverSide = "net.automods.sidekeyrelay.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }
}
