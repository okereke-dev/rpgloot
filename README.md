# RPGLoot

Plugin de Paper para Minecraft 1.21.4 que añade un sistema de rarezas a armas obtenidas por drop de mobs hostiles.

## Rarezas

| Rareza | Color | Bonus Stats |
|---|---|---|
| Common | Gris | Ninguno |
| Uncommon | Amarillo | 1 stat bajo |
| Rare | Violeta | 1 stat decente |
| Hero | Verde | 1 stat muy bueno |
| Legendary | Naranja | 2 stats buenos |

Cada arma mantiene los stats vanilla de Minecraft como base. Las rarezas superiores aplican un multiplicador aleatorio dentro de su rango sobre el daño y velocidad de ataque.

## Bonus Stats (armas)

- **Lifesteal** — cura un porcentaje del daño infligido
- **Crit Chance** — probabilidad de golpe crítico (x1.5 daño)
- **Knockback** — empuje extra al golpear
- **Sweep Damage** — daño extra en golpe de área

## Obtención

Las armas con rareza solo se obtienen por drop al matar mobs hostiles. La probabilidad de drop es configurable en `config.yml` (por defecto 8%).

## Comandos

| Comando | Descripción |
|---|---|
| `/rpgloot get [rareza] [material]` | Genera un arma con la rareza y material indicados |
| `/rpgloot getall` | Genera una arma de cada rareza |

**Rarezas válidas:** `common`, `uncommon`, `rare`, `hero`, `legendary`

**Permiso requerido:** `rpgloot.admin`

## Configuración

```yaml
# config.yml
drop-chance: 0.08  # 0.0 a 1.0
```

## Instalación

1. Descarga `rpgloot.jar` desde [Releases](../../releases)
2. Cópialo a la carpeta `plugins/` de tu servidor Paper 1.21.4
3. Reinicia el servidor

## Compilación

Requiere Java 21 y Maven.

```bash
mvn clean package
```

El jar se genera en `target/rpgloot.jar`.
