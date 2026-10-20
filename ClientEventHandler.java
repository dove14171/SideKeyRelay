package net.automods.sidekeyrelay;

import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

/**
 * 客户端输入修复核心。
 *
 * Forge 1.12.2 的 Minecraft#runTickMouse 中，每个 LWJGL 鼠标事件的处理顺序是：
 *   1. ForgeHooksClient#postMouseEvent —— 发布 MouseEvent（此时先于后面两步！）
 *   2. KeyBinding#setKeyBindState(button - 100, state) —— 更新按下状态
 *   3. 按下时调用 KeyBinding#onTick(button - 100) —— 累加 pressTime
 *   4. FMLCommonHandler#fireMouseInput —— 发布 InputEvent.MouseInputEvent
 *
 * 而 1.12.2 的 KeyBinding#isPressed() 是"边沿触发"：读取并递减 pressTime。
 * 如果照原模组的思路直接在 MouseEvent 回调里补发 KeyInputEvent，
 * 此刻 pressTime 还没累加，AE2 终端这类轮询 isPressed() 的监听器读到的是 false，
 * 症状就是"还需要再按一个其他按键才会触发"。
 *
 * 因此这里分两步：
 *   - MouseEvent 回调只记录"本条事件是不是侧键（button > 2，按下、松开都算）"；
 *   - 等紧随其后、在同一轮鼠标事件处理完毕后发布的 MouseInputEvent，
 *     再补发 KeyInputEvent —— 此时 setKeyBindState / onTick 已执行，
 *     按下与松开两种情况监听器都能立刻读到正确状态。
 *
 * 两个事件都发布在同一个总线上（FML 已将 FMLCommonHandler#bus() 指向
 * MinecraftForge.EVENT_BUS），一个 handler 注册即可同时收到。
 * 普通左/中/右键与滚轮、移动（button &lt;= 2 或 -1）不补发，行为完全不变。
 */
public class ClientEventHandler {

    /** 当前这条鼠标事件是否为侧键（按下或松开） */
    private boolean sideButtonEvent = false;

    @SubscribeEvent
    public void onMouseButton(MouseEvent event) {
        // 只记录标记，不在这里补发（此时 KeyBinding 状态尚未更新）
        sideButtonEvent = event.getButton() > 2;
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        // 紧跟在 setKeyBindState / onTick 之后发布：按下、松开都补发
        if (sideButtonEvent) {
            sideButtonEvent = false;
            MinecraftForge.EVENT_BUS.post(new InputEvent.KeyInputEvent());
        }
    }
}
