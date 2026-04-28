-- MySQL 5.7 compatible
-- beginner 仅影响后续新建角色，不会补发给已有角色

DELETE FROM `beginner`
WHERE `item_id` IN (30652, 30653);

INSERT INTO `beginner` (
  `item_id`,
  `count`,
  `charge_count`,
  `enchantlvl`,
  `item_name`,
  `activate`,
  `bless`
) VALUES
  (30652, 1, 0, 0, '掛機控制器', 'A', 1),
  (30653, 1, 0, 0, '一鍵傳送', 'A', 1);
