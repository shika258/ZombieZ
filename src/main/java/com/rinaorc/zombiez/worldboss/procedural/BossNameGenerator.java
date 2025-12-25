package com.rinaorc.zombiez.worldboss.procedural;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Générateur de noms procéduraux pour les boss
 * Combine préfixes + base + suffixes pour créer des noms uniques
 */
public class BossNameGenerator {

    // Préfixes de titre (rang/puissance)
    private static final String[] TITLE_PREFIXES = {
        "l'Ancien", "le Grand", "le Sombre", "l'Immortel", "le Maudit",
        "le Terrible", "le Redouté", "l'Impitoyable", "le Funeste", "le Sinistre",
        "l'Infâme", "le Dévastateur", "l'Éternel", "le Putride", "le Vorace",
        "le Déchu", "l'Abyssal", "le Corrompu", "l'Ombre de", "le Fléau"
    };

    // Préfixes d'origine
    private static final String[] ORIGIN_PREFIXES = {
        "des Profondeurs", "de la Nuit", "des Cendres", "du Néant",
        "de l'Abîme", "des Ténèbres", "de la Désolation", "du Chaos",
        "des Limbes", "de la Pestilence", "de l'Oubli", "du Crépuscule",
        "des Ruines", "de la Famine", "du Tombeau", "de l'Agonie"
    };

    // Suffixes de puissance
    private static final String[] POWER_SUFFIXES = {
        "le Destructeur", "le Conquérant", "l'Annihilateur", "le Massacreur",
        "le Dominateur", "le Ravageur", "l'Éventreur", "le Faucheur",
        "le Broyeur", "le Dévoreur", "le Tortionnaire", "l'Écorcheur"
    };

    // Noms d'âmes/entités (pour variation du nom de base)
    private static final String[] SOUL_NAMES = {
        "Azrael", "Mortis", "Nexus", "Voidon", "Malkor",
        "Draven", "Scorn", "Blight", "Plague", "Wraith",
        "Specter", "Phantom", "Shade", "Doom", "Dread",
        "Fang", "Claw", "Gore", "Rot", "Decay"
    };

    // Adjectifs de modification
    private static final String[] MODIFIERS = {
        "Ancestral", "Primitif", "Colossal", "Titanesque", "Monstrueux",
        "Cauchemardesque", "Apocalyptique", "Légendaire", "Mythique", "Antique",
        "Féroce", "Sauvage", "Bestial", "Démoniaque", "Infernal"
    };

    /**
     * Génère un nom de boss procédural
     */
    public static ProceduralName generate(String baseName, BossTrait[] traits) {
        return generate(baseName, traits, ThreadLocalRandom.current());
    }

    /**
     * Génère un nom de boss procédural avec un Random spécifique (pour seed)
     */
    public static ProceduralName generate(String baseName, BossTrait[] traits, java.util.Random random) {

        StringBuilder fullName = new StringBuilder();
        StringBuilder titleName = new StringBuilder();

        // Décider de la structure du nom (variété)
        int structure = random.nextInt(5);

        // Couleur basée sur le trait principal
        String colorCode = traits.length > 0 ? traits[0].getColorCode() : "§c";

        switch (structure) {
            case 0 -> {
                // "[Trait] Le Boucher des Profondeurs"
                String modifier = MODIFIERS[random.nextInt(MODIFIERS.length)];
                String origin = ORIGIN_PREFIXES[random.nextInt(ORIGIN_PREFIXES.length)];
                fullName.append(modifier).append(" ").append(baseName).append(" ").append(origin);
                titleName.append(colorCode).append("§l").append(modifier.toUpperCase()).append(" ")
                    .append(baseName.toUpperCase()).append(" ").append(origin.toUpperCase());
            }
            case 1 -> {
                // "Azrael, l'Ancien Boucher"
                String soul = SOUL_NAMES[random.nextInt(SOUL_NAMES.length)];
                String title = TITLE_PREFIXES[random.nextInt(TITLE_PREFIXES.length)];
                fullName.append(soul).append(", ").append(title).append(" ").append(baseName);
                titleName.append(colorCode).append("§l").append(soul.toUpperCase()).append(", ")
                    .append(title.toUpperCase()).append(" ").append(baseName.toUpperCase());
            }
            case 2 -> {
                // "Le Grand Boucher Destructeur"
                String title = TITLE_PREFIXES[random.nextInt(TITLE_PREFIXES.length)];
                String suffix = POWER_SUFFIXES[random.nextInt(POWER_SUFFIXES.length)];
                fullName.append(title).append(" ").append(baseName).append(" ").append(suffix);
                titleName.append(colorCode).append("§l").append(title.toUpperCase()).append(" ")
                    .append(baseName.toUpperCase()).append(" ").append(suffix.toUpperCase());
            }
            case 3 -> {
                // "[Trait1] & [Trait2] Boucher"
                if (traits.length >= 2) {
                    fullName.append(baseName).append(" ")
                        .append(traits[0].getDisplayName()).append(" et ")
                        .append(traits[1].getDisplayName());
                    titleName.append(colorCode).append("§l")
                        .append(baseName.toUpperCase()).append(" ")
                        .append(traits[0].getColorCode())
                        .append(traits[0].getDisplayName().toUpperCase())
                        .append(" §7& ")
                        .append(traits[1].getColorCode())
                        .append(traits[1].getDisplayName().toUpperCase());
                } else {
                    String origin = ORIGIN_PREFIXES[random.nextInt(ORIGIN_PREFIXES.length)];
                    fullName.append(baseName).append(" ").append(origin);
                    titleName.append(colorCode).append("§l")
                        .append(baseName.toUpperCase()).append(" ").append(origin.toUpperCase());
                }
            }
            default -> {
                // "Boucher Alpha/Beta/Gamma + numéro"
                String[] greekLetters = {"Alpha", "Beta", "Gamma", "Delta", "Omega", "Prime", "Apex"};
                String letter = greekLetters[random.nextInt(greekLetters.length)];
                int number = random.nextInt(99) + 1;
                fullName.append(baseName).append(" ").append(letter).append("-").append(number);
                titleName.append(colorCode).append("§l")
                    .append(baseName.toUpperCase()).append(" §7[§e").append(letter.toUpperCase())
                    .append("-").append(number).append("§7]");
            }
        }

        return new ProceduralName(fullName.toString(), titleName.toString(), colorCode);
    }

    /**
     * Représente un nom procédural généré
     */
    public record ProceduralName(String displayName, String titleName, String colorCode) {}
}
