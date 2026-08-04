# VineMod — forked from Zipline (MIT)

Base package: `com.example.vinemod` — modid: `vinemod`. Đổi lại tuỳ ý trong:
- `META-INF/neoforge.mods.toml` (modId, displayName)
- tên package Java (đổi bằng IDE "rename package", không nên đổi tay bằng sed
  sau khi mở project vì có nhiều chỗ tham chiếu chéo)

## Giữ nguyên gần như 100% (không cần đụng vào)
- `mixin/PlayerMixin.java` + `duck/ZiplinePlayerDuck.java` — lưu state (cable, progress,
  speed, directionFactor, lastDir, actuallyUsing) trực tiếp trên `Player` qua duck interface.
- `mixin/ServerGamePacketListenerImplMixin.java` — bypass anti-cheat
  (`getMaximumFlyingTicks`, `isInPostImpulseGraceTime`, `shouldCheckPlayerMovement`)
  khi item đang dùng có tag `ATTACHMENT`. **Đây là phần quan trọng nhất, đừng sửa
  trừ khi bạn hiểu rõ vì sao nó cần thiết.**
- `mixin/GameRendererMixin.java` — camera tilt lúc bám dây.
- `mixin/HumanoidModelMixin.java` — bẻ tư thế tay khi `state.isUsingItem`.
- `mixin/ItemMixin.java` — hook `use/onUseTick/releaseUsing/getUseDuration/getUseAnimation/inventoryTick`
  trên `Item.class` dựa theo tag. Đây là lý do bạn chỉ cần đổi nội dung tag
  (`data/vinemod/tags/item/attachment.json`) để đổi "vật kích hoạt" mà không cần sửa Java.

## Cần viết lại / thay thế
- `Cables.java` + `Cable.java` — giữ interface, nhưng **provider đang đăng ký duy nhất
  là `compat/connectiblechains/ConnectibleChainsCompat.java`** (đọc chain-entity của mod
  ConnectibleChains). `StraightCable.java` hiện KHÔNG có provider nào dùng — code chết.
  → Việc chính bạn cần làm: viết `VineCableProvider` quét block dây leo trong world
  (flood-fill các block liền kề có tag vine) rồi trả về `StraightCable` giữa 2 đầu mút,
  sau đó `Cables.registerProvider(...)` trong `VineMod.init()`.
- `logic/ZiplineLogic.java` — đổi tên biến/sound cho hợp ngữ cảnh dây leo nếu muốn,
  logic di chuyển dọc theo `Cable` giữ nguyên được.
- `data/vinemod/tags/item/attachment.json` — đổi từ pickaxe/wrench sang
  `minecraft:stick` (chắc chắn chạy) hoặc thử `minecraft:air` (tay không — cần test
  thực tế vì tuỳ version MC, right-click tay không có thể không gọi `Item.use()`;
  nếu không fire, hook thêm `PlayerInteractEvent.RightClickEmpty` để tự gọi
  `player.startUsingItem(hand)`).
- `mixin/ItemInHandLayerMixin.java` / `mixin/ItemInHandRendererMixin.java` — chỉ vẽ
  vật phẩm trong tay; nếu dùng tay không (không phải stick) thì gần như không cần,
  có thể xoá hoặc để nguyên (vô hại vì ItemStack rỗng không có gì để vẽ).

## Optional
- `compat/connectiblechains/*` — chỉ cần nếu bạn muốn dây leo tương tác được với
  chain của mod ConnectibleChains. Nếu không cần, xoá cả package này và dòng
  `Services.PLATFORM.isModLoaded("connectiblechains")` trong `VineMod.init()`.
- `config/ClothConfigIntegration.java` — cần Cloth Config API làm dependency;
  xoá nếu không muốn màn hình config trong game.

## License
MIT — xem `LICENSE`. Bắt buộc giữ lại credit tác giả gốc (Evan, Tomate0613) theo
điều khoản MIT khi bạn phát hành lại dưới tên khác.
