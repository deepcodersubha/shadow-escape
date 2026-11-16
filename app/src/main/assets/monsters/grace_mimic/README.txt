Grace Mimic (False Site of Grace) Sprite Assets
==================================================

Drop your sprite assets into this directory to replace the procedural phantom placeholder.

Supported formats:
1. Individual animation strips (standard format):
   - idle.png   (Frame count: 4 or customizable)
   - run.png    (Frame count: 8 or customizable)
   - attack.png (Frame count: 8 or customizable)
   - hit.png    (Frame count: 4 or customizable)
   - death.png  (Frame count: 4 or customizable)

2. Or a single master sprite sheet:
   - grace_mimic.png / grace_mimic_sheet.png

The EnemyAnimator automatically checks this directory upon launch. If assets are present, it seamlessly binds and animates them; otherwise, it falls back to the ethereal spectral Grace Phantom.
