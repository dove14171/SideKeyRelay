# SideKeyRelay（侧键中继）

适用于 **Minecraft Forge 1.12.2** 的小型客户端模组，解决鼠标侧键快捷键"要再按一个键才生效"的问题。

> 改名说明：本项目曾用名 `MouseSideButtonFix`。为避免与 [asdflj 的 MouseSideButtonFix（Forge 1.7.10）](https://www.mcmod.cn/class/26011.html) 重名，现已更名为 **SideKeyRelay**，modid 为 `sidekeyrelay`。两者只有修复思路同源，代码实现（事件时序、补发时机）是为 1.12.2 重新编写的，**不是同一个模组，也请只保留其中一个**。

## 解决的问题

把快捷键绑定到**鼠标侧键**（鼠标键 4 / 5，即 `button > 2`）后，快捷键不会立刻生效——必须先按下任意其他按键才触发。AE2 终端类模组的快捷键尤其明显。

本模组的修复目标：**侧键按下、松开都立刻触发**绑定的快捷键，无需再"搭"别的键。

## 原理简述

FML 1.12.2 只在键盘事件循环里发布 `InputEvent.KeyInputEvent`。侧键事件走的是鼠标循环（`Minecraft#runTickMouse`），`KeyBinding` 状态虽然被更新了，但只在 `KeyInputEvent` 回调中轮询快捷键的模组（如 AE2）感知不到这次按压。

修复方式（"中继"）：

1. 在 `MouseEvent` 回调中**只记录标记**（`getButton() > 2`，按下与松开都算）；
2. 等每条鼠标事件处理完毕后发布的 `InputEvent.MouseInputEvent`（此时 `setKeyBindState` 与 `onTick` 均已执行），再补发一个 `KeyInputEvent`，唤醒轮询型监听器。

> 注意：1.12.2 的 `KeyBinding#isPressed()` 是边沿触发（读 `pressTime` 并递减），而 `MouseEvent` 发布在 `onTick` 之前。若直接在 `MouseEvent` 里补发，`isPressed()` 永远读到 0——这正是 1.7.10 的实现照搬到 1.12.2 会失效的原因。

## 兼容性

| 项目 | 值 |
| --- | --- |
| Minecraft | 1.12.2 |
| Forge | 14.23.5.2847+（任意 1.12.2 正式版 Forge 均可） |
| 生效端 | 仅客户端（服务端无需安装） |
| modid | `sidekeyrelay` |

## 安装

把 `SideKeyRelay-<版本>.jar` 放进 `.minecraft/mods` 即可。

> 若之前装过旧名 `MouseSideButtonFix-*.jar`，请先删除——旧 jar 里的类与新 jar 类名不同，但功能重复，同时存在会重复补发事件。

## 从源码构建

环境要求：

- **JDK 8**（1.12.2 ForgeGradle 2.3 必须用 Java 8）
- **Gradle 4.10.3**（不建议更新版本，ForgeGradle 2.3-SNAPSHOT 不兼容 Gradle 5+）

```bash
gradle build
```

产物在 `build/libs/SideKeyRelay-<版本>.jar`。首次构建会自动下载 Forge 与映射，需要网络。

## 目录结构

```
src/main/java/net/automods/sidekeyrelay/
├── SideKeyRelay.java        # @Mod 主类
├── CommonProxy.java         # 服务端代理（空实现）
├── ClientProxy.java         # 客户端代理：注册事件监听
└── ClientEventHandler.java  # 核心：侧键标记 + 补发 KeyInputEvent
src/main/resources/
├── mcmod.info               # 模组元数据
└── pack.mcmeta              # 资源包元数据
```

## 致谢

- 思路参考：asdflj —— MouseSideButtonFix（Forge 1.7.10）
- 参考页面：[mcmod.cn #26011](https://www.mcmod.cn/class/26011.html)

## 许可

发布前请在本仓库补充 LICENSE 文件（如 MIT、Apache-2.0 或自定条款），并在上方注明。
