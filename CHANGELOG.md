# Alterna: Echoes Below — Danh sách cập nhật

## Bản cập nhật: Lichen Savanna & Armor Overlord

> ⚠️ Bản này thay đổi cách sinh thế giới bên trong khe nứt khổng lồ. Hãy **sao lưu thế giới** trước khi cập nhật. Vùng mới chỉ xuất hiện ở các chunk chưa được tạo.

Trong bản này có:

- một vùng mới bên trong khe nứt: **Lichen Savanna**;
- ba sinh vật mới: **Cragback**, **Tetra** và trùm **Armor Overlord** (sống trong rừng Rootshroom);
- bộ giáp, đồ ăn, vũ khí và dụng cụ mới;
- **Máy quét sinh học** (Bio-Scanner) có giọng đọc AI.

---

## 1. Vùng mới: Lichen Savanna

Một cao nguyên đồng cỏ màu tím đỏ nằm trên các gờ đá lớn sâu bên trong khe nứt. Ở đây có:

- những gốc nấm Weavecap khổng lồ,
- bụi Violet,
- các ao nước nhỏ,
- những đàn Cragback đang gặm cỏ,
- những đàn Tetra bay lượn và làm tổ trên vách đá.

### Khối và cây cối

| Tên | Công dụng |
|---|---|
| **Lichen Turf** (Thảm Lichen) | Lớp phủ mặt đất màu tím. Nó là địa y chứ không phải cỏ, nên **mọc lan trong bóng tối hoàn toàn**. Chỉ chết thành đất khi bị khối đặc đè lên. Cragback gặm nó thành đất, sau đó nó tự mọc lại. |
| **Lichen Grass / Tall Lichen Grass** | Cỏ địa y, là thức ăn chính của Cragback. |
| **Lichen Stone** (Đá Lichen) | Đá phủ địa y. Chế tạo: Đá + Lichen Grass (không cần xếp hình). |
| **Weavecap** (Nấm Bện) | Nấm khổng lồ có thân bện từ nhiều sợi rễ. Có đủ bộ gỗ: thân, thân đã bóc vỏ, hyphae, ván, cầu thang, phiến, hàng rào, cổng, cửa, cửa sập, nút bấm, tấm áp lực. Ngoài ra còn mũ nấm (Weavecap Cap) và phiến nấm (Weavecap Gills). |
| **Weavecap Sprout** (Mầm Weavecap) | Mọc thành cây Weavecap nhỏ. Là nấm nên **mọc được trong bóng tối**, nhưng cần khoảng trống phía trên. Dùng được bột xương. |
| **Ember Cap** (Nấm Ember) | Nấm nhỏ phát sáng. **Ăn vào được Nhìn đêm 20 giây.** Chế ra thuốc nhuộm cam. |
| **Violet Bush / Violet Leaves** | Bụi cây và lá màu tím. Chế ra thuốc nhuộm tím. |
| **Tetra Nest** (Tổ Tetra) | Xem phần Tetra bên dưới. Chế tạo: 2 lông vũ + 3 que gỗ. |

---

## 2. Sinh vật mới

### 🦏 Cragback

Loài thú ăn cỏ khổng lồ, bọc giáp, đi thành đàn trên Lichen Savanna.

- **Hiền lành.** Nó không bao giờ tấn công trước, nhưng **hạ sừng và húc** bất cứ ai đánh nó. Bình tĩnh lại rất nhanh khi mối đe dọa biến mất.
- **Da cực dày:** 80 máu, 10 giáp, gần như miễn kháng đẩy lùi. Tên chỉ gây khoảng 40% sát thương; đòn yếu (≤ 4) chỉ gây khoảng 60%.
- **Lưng là khối rắn**, người chơi có thể đứng lên lưng nó.
- **Gặm cỏ và đi uống nước:** thường xuyên gặm Lichen Grass và tự đi tới ao gần nhất để uống.
- **Tự rụng Crag Plate** (Mảng giáp Crag) theo thời gian, giống tatu rụng vảy. Bạn lấy được nguyên liệu làm giáp mà không cần giết nó.
- **Nhân giống** bằng Lichen Grass hoặc lúa mì. Con non có model riêng và lớn nhanh hơn khi được gặm cỏ.
- **Chuyển động procedural:** từng chân đặt đúng xuống mặt đất, bước chân khớp với tốc độ di chuyển, thân nghiêng theo địa hình, xoay người chậm. Đầu có hitbox riêng.
- **Rơi ra:** da (1–3), Raw Crag Shank (2–4), Crag Plate (0–2).

### 🐦 Tetra

Loài bay có bốn cánh, sống thành đàn trên vách đá quanh savanna.

- **Bay theo đàn**, đội hình chữ V. Thỉnh thoảng nó bám vào vách đá dựng đứng để quan sát rồi lại cất cánh. Cất cánh và hạ cánh dứt khoát, không bị nhấp nháy giữa đi và bay.
- **Săn cá:** lao xuống bắt cá trong ao, nhặt cá rơi trên đất.
- **Hái quả mọng:** ngậm White Currant / Sweet Berry trong mỏ (hiện đúng vật phẩm như con cáo vanilla), ăn sau hoặc thả rơi khi đang bay.
- **Làm tổ:**
  - Cho hai con trưởng thành ăn cá sống để nhân giống. Một con sẽ bay đi đẻ trứng vào tổ (tự làm tổ nếu chưa có).
  - Mỗi tổ chứa tối đa 3 trứng, trứng tự nở thành Tetra con.
- **Bảo vệ tổ:** Tetra không tự tấn công ai, nhưng nếu bạn **lấy trứng hoặc phá tổ còn trứng**, cả đàn gần đó sẽ lao vào bạn.
- **Thuần hóa:**
  - Cho Tetra **con** ăn cá sống, tỉ lệ thuần là 1/3 mỗi lần cho ăn.
  - Trứng do người chơi tự đặt vào tổ được tính là đã chăm sóc; con nở từ tổ đó **nhận bạn làm chủ** ngay khi nở.
- **Tetra đã thuần:**
  - Bay theo bạn và đánh cùng bạn.
  - Chuột phải (tay không cầm cá) để bắt nó ngồi hoặc đi theo.
  - Cho ăn cá để hồi máu.
- **Rơi ra:** lông vũ (1–3).

### 🕷️ Armor Overlord (trùm rừng Rootshroom)

Kẻ săn mồi đầu bảng trong **rừng Rootshroom**:

- dạng nhện cao khoảng **5 block**;
- mang một chiếc **mũ nấm bọc giáp** trên **4 chân khổng lồ**;
- có **cụm não phát sáng** dưới vành mũ và **2 tay móc dài**.

Xuất hiện sẵn khi thế giới được tạo, khoảng 1–2 con mỗi khu rừng.

**Hành vi**

- **Săn mọi thứ biết cử động:** người chơi, động vật, zombie.
- **Chỉ nhìn được phía trước** (góc khoảng 110°, xa 24 block):
  - nếu bạn **ngồi thụp**, nó chỉ thấy bạn trong vòng 7 block;
  - nếu bạn đứng sát chân nó, nó luôn cảm nhận được.
- **Mất dấu:** khi không thấy bạn vài giây (ra sau lưng, nấp sau cây…), nó đi tới chỗ thấy bạn lần cuối, đảo quanh tìm một lúc rồi **bỏ cuộc**.
- **Xoay người chậm bằng 4 chân:** thân chỉ được vặn một chút so với chỗ chân đang bám. Muốn quay tiếp, nó phải nhấc từng cặp chân đặt sang hướng mới. Đây là sơ hở để bạn **vòng ra sau lưng**.
- Gầm lên khi phát hiện con mồi. Thanh máu boss chỉ hiện cho người đang bị nó săn.
- **Phá lá cây** khi đi qua (tuân theo luật `mobGriefing`).
- Ăn con mồi xong thì hồi máu.

**Đòn tấn công**

| Đòn | Khi nào | Tác dụng |
|---|---|---|
| **Móc kéo** | Bạn ở xa 5–9 block, ngay trước mặt nó | Giơ móc lên (có dấu hiệu báo trước), phóng tay móc ra **kéo bạn lại gần**. |
| **Chém đôi** | Bạn ở gần (dưới 5 block) | Hai nhát chém liên tiếp bằng hai lưỡi móc. |
| **Dậm đất** | Bạn đứng dưới thân nó | Nhổm lên rồi đạp mạnh xuống, gây sát thương vùng và **hất văng**. |

**Điểm yếu & cách đánh**

- **Mũ nấm là giáp:** đánh vào mũ, hoặc **đánh từ trên cao xuống (kể cả bắn tên)**, chỉ gây khoảng **15%** sát thương.
- **Chân** là kitin dày: gây khoảng **60%** sát thương.
- **Cụm não phát sáng** dưới vành mũ nhận **x2 sát thương**.
- **Đánh lén** (nó chưa phát hiện bạn) bằng cận chiến: nhân thêm **x1.5**.
- **Cách đánh gợi ý:** ngồi thụp, vòng ra sau lưng, chui xuống dưới thân rồi đánh vào não từ bên dưới. Chú ý né cú dậm đất (nó nhổm người lên trước khi đạp).
- **Hitbox thật:** mỗi chân có hitbox riêng và đẩy bạn ra như cột trụ; thân, mũ và não có hitbox bám theo model. Bạn **đi được vào dưới thân nó**.
- **Chỉ số:** 150 máu, 6 giáp, miễn đẩy lùi, không tự biến mất.
- **Rơi ra:**
  - Móc Overlord (1–2)
  - Mảnh Mai Mũ Nấm (3–5, Looting tăng thêm)
  - xương, mắt nhện, bột đá phát sáng
  - 40 kinh nghiệm

---

## 3. Vật phẩm mới

### Đồ ăn

| Vật phẩm | Hồi | Ghi chú |
|---|---|---|
| **Raw Crag Shank** (Đùi Crag sống) | 4 đói | Rơi từ Cragback. |
| **Cooked Crag Shank** (Đùi Crag chín) | 9 đói, độ no cao | Nấu bằng lò nung, lò hun khói hoặc lửa trại. |
| **Ember Cap** | 2 đói | Cho **Nhìn đêm** 20 giây. |

### Giáp Cragplate

- **Chế tạo** từ **Crag Plate** theo công thức giống giáp vanilla.
- **Chỉ số:**
  - độ bền hệ số 28, giáp tổng 19 (mũ 3, áo 7, quần 6, giày 3);
  - **độ cứng 2.5** và **kháng đẩy lùi**: nặng và chắc như con thú mà nó được làm ra.
- Sửa bằng Crag Plate.
- Texture 2D và 3D theo phong cách vanilla, có các lớp giáp chồng lên nhau.

### 🪝 Móc Leo Overlord (Overlord Grapple)

- **Chế tạo:** Móc Overlord + dây (string) + thỏi sắt, xếp chéo:
  ```
  . . H
  . S .
  I . .
  ```
- **Cách dùng — giữ chuột phải:**
  - bắn móc ra (tầm khoảng 36 block);
  - móc cắm vào block thì bạn **được kéo tới**;
  - tới nơi thì **treo lơ lửng** bám trên vách cho tới khi thả chuột.
- Thả chuột là nhả móc.
- **Không mất máu khi rơi** trong lúc đang được kéo.
- Rất hợp để di chuyển trong khe nứt khổng lồ.
- Độ bền 256, sửa bằng Móc Overlord.

### 🛡️ Khiên Mũ Nấm (Capshell Shield)

- **Chế tạo:** 6 Mảnh Mai Mũ Nấm + 1 thỏi sắt, xếp giống khiên vanilla:
  ```
  C I C
  C C C
  . C .
  ```
- **So với khiên vanilla:**
  - chặn tên/vật bay ở **góc rộng 135°** (khiên thường 90°);
  - **mòn chậm hơn**, độ bền **560** (khiên thường 336);
  - giơ khiên nhanh hơn một chút;
  - rìu làm vô hiệu khiên trong thời gian ngắn hơn.
- Model 3D hình mũ nấm, có răng xương ở mép và núm giữa ánh vàng.
- Sửa bằng Mảnh Mai Mũ Nấm.

### Khác

- **Crag Plate**: nguyên liệu giáp, do Cragback tự rụng hoặc rơi ra khi giết.
- **Tetra Egg** (Trứng Tetra): đặt vào Tổ Tetra để ấp.
- **Móc Overlord**, **Mảnh Mai Mũ Nấm**: nguyên liệu từ Armor Overlord.
- **Trứng sinh** cho Cragback, Tetra và Armor Overlord.

---

## 4. 🔬 Máy quét sinh học (Bio-Scanner)

**Chế tạo:**
```
C G C      C = thỏi đồng, G = kính tấm
I R I      I = thỏi sắt,  R = đá đỏ
I A I      A = mảnh thạch anh tím
```

**Cách dùng:** cầm máy, **giữ chuột phải** nhắm vào sinh vật hoặc thực vật trong khe nứt.

**Khi đang quét**
- Một tia quét bắn ra từ tay.
- **Sinh vật** được phủ một lớp màn quét lên cơ thể.
- **Cây cối** được bao trọn bằng một lớp vỏ ánh sáng.

**Khi quét xong**
- **Bảng thông tin** trượt ra bên phải màn hình, ghi rõ loài là động vật hay thực vật.
- **Giọng đọc AI** tiếng Anh kiểu PDA trong Subnautica đọc mô tả, chữ chạy theo giọng đọc.

**Dữ liệu có sẵn (20 mục):**
- **Sinh vật:** Cragback, Tetra, Tổ Tetra, Climbing Zombie, Armor Overlord. Mục Armor Overlord có gợi ý cách lén ra sau lưng và đánh vào não.
- **Thực vật:** Weavecap, Lichen Grass, Ember Cap, Violet Bush, White Currant, Star Lily, Canopra, Goblet Tree, Rootshroom, Enoki, Cloudberry, Daze, Wild Moss, Purple Sugar Cane, Vitalroot.

Có bản dịch tiếng Anh và tiếng Việt (giọng đọc chỉ có tiếng Anh).

---

## 5. Âm thanh

Âm thanh riêng cho:

- **Tetra:** kêu, bị thương, chết, đập cánh.
- **Cragback:** gầm gừ, bị thương, chết.
- **Armor Overlord:**
  - tiếng chitin lách cách, gầm, bị thương, chết;
  - bước chân dậm đất (kèm tiếng bước chân của chính block nó giẫm lên);
  - tiếng móc vụt, dậm đất, mai giáp đỡ đòn.
- **Máy quét:** tiếng quét, quét xong, quét lỗi, bảng thông tin trượt ra, cùng giọng đọc cho từng mục.

---

## 6. Ghi chú kỹ thuật

- Cragback, Tetra và Armor Overlord dùng **hoạt ảnh procedural** (chân bám đất bằng IK, thân nghiêng theo địa hình), không dùng hoạt ảnh định sẵn.
- Sinh vật lớn dùng **hitbox nhiều phần** (PartEntity):
  - Cragback: thân + đầu;
  - Armor Overlord: 4 chân + thân + mũ + não.
- Toàn bộ chữ có bản **English** và **Tiếng Việt**.
- Yêu cầu: Minecraft 26.1.x, NeoForge 26.1.2.87, Java 25.
