# Implementation Plan: 离线数独 App

## Overview

在现有空 Android 单模块项目中实现一个单 Activity 数独 App。核心逻辑使用纯 Java 类封装，UI 使用原生 View 编写，保持免登录、无广告、无网络权限。

## Requirements

- 支持四种难度：初级、中级、高级、噩梦。
- 每局有计时，暂停停止计时，开始继续计时。
- 每局最多允许 3 次错误，底部显示错误次数。
- 离线生成题目并校验用户输入。
- 提供单元测试和本地编译验收。

## Architecture Changes

- `docs/prd.md`：产品需求文档。
- `docs/implementation-plan.md`：实现计划。
- `app/src/main/java/com/kevy/sudoku/game/*`：数独生成、求解、题目模型、难度模型。
- `app/src/main/java/com/kevy/sudoku/MainActivity.java`：游戏状态、计时、难度切换、输入控制。
- `app/src/main/java/com/kevy/sudoku/SudokuBoardView.java`：棋盘绘制和格子选择。
- `app/src/main/res/values/*`：应用名称、颜色、无 ActionBar 主题。
- `app/src/main/AndroidManifest.xml`：声明入口 Activity，不增加网络权限。
- `app/src/test/java/com/kevy/sudoku/game/*`：核心逻辑单元测试。

## Implementation Steps

### Phase 1: Documentation

1. **Create PRD** (File: `docs/prd.md`)
   - Action: 写明产品定位、核心需求、非功能需求、验收标准。
   - Why: 先固定范围，避免实现时漂移。
   - Dependencies: None.
   - Risk: Low.

2. **Create implementation plan** (File: `docs/implementation-plan.md`)
   - Action: 拆分核心逻辑、UI、资源、验收测试。
   - Why: 给并行实现提供稳定边界。
   - Dependencies: PRD.
   - Risk: Low.

### Phase 2: Core Game Logic

3. **Implement Sudoku model and difficulty** (Files: `app/src/main/java/com/kevy/sudoku/game/Difficulty.java`, `SudokuPuzzle.java`)
   - Action: 定义难度、不可变题目、给定格、答案校验和完成判断。
   - Why: UI 只依赖稳定模型，不直接处理解题算法。
   - Dependencies: Phase 1.
   - Risk: Low.

4. **Implement generator and solver** (Files: `SudokuGenerator.java`, `SudokuSolver.java`)
   - Action: 生成完整解，按难度挖空，验证唯一解。
   - Why: 保证题目可玩且可验收。
   - Dependencies: Step 3.
   - Risk: Medium, because unique-solution generation can be slow if not bounded.

5. **Add unit tests** (Files: `app/src/test/java/com/kevy/sudoku/game/*`)
   - Action: 覆盖难度、题目一致性、唯一解、校验和完成判断。
   - Why: 防止生成器和校验逻辑后续回归。
   - Dependencies: Step 4.
   - Risk: Low.

### Phase 3: Game UI

6. **Add main activity** (File: `app/src/main/java/com/kevy/sudoku/MainActivity.java`)
   - Action: 实现难度切换、新局、暂停/开始、计时、错误次数、胜负提示。
   - Why: 承载完整用户流程。
   - Dependencies: Phase 2 public API.
   - Risk: Medium, because state transitions must be consistent.

7. **Add board view** (File: `app/src/main/java/com/kevy/sudoku/SudokuBoardView.java`)
   - Action: 绘制 9x9 棋盘、初始数字、用户数字、选中格，处理点击。
   - Why: 保证棋盘正方形且不依赖复杂布局。
   - Dependencies: Step 6.
   - Risk: Low.

8. **Wire resources and manifest** (Files: resources and manifest)
   - Action: 设置应用入口、中文名称、主题颜色，不声明网络权限。
   - Why: 让 App 可启动并符合无广告离线要求。
   - Dependencies: Step 6.
   - Risk: Low.

## Testing Strategy

- Unit tests: `./gradlew.bat testDebugUnitTest`
- Build: `./gradlew.bat assembleDebug`
- Manual acceptance from code path:
  - 新局后计时开始。
  - 暂停后计时停止且棋盘不可操作。
  - 错误输入累计到底部错误次数。
  - 第 3 次错误后本局结束。
  - 完成所有格后触发通关提示。

## Risks & Mitigations

- **Risk**: 噩梦难度生成过慢。
  - Mitigation: 使用随机完整解加唯一解验证，并限制挖空尝试次数。

- **Risk**: 暂停、失败、完成状态互相覆盖。
  - Mitigation: Activity 维护明确状态字段：`paused`、`gameOver`、`puzzle`、`elapsedBeforeStartMs`。

- **Risk**: 错误数字被写入导致题面污染。
  - Mitigation: 输入前用 `SudokuPuzzle.isCorrect()` 校验，错误只计数不写入。

## Success Criteria

- [ ] PRD 和实现计划存在且覆盖需求。
- [ ] App 有可启动入口 Activity。
- [ ] 四种难度可选择并生成新局。
- [ ] 计时暂停/开始行为正确。
- [ ] 错误次数底部显示，3 次后结束。
- [ ] 核心逻辑单元测试通过。
- [ ] Debug APK 编译通过。
