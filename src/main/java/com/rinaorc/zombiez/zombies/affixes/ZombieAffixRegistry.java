package com.rinaorc.zombiez.zombies.affixes;

import java.util.Collection;

/**
 * Wrapper pour accéder au registre d'affixes depuis ZombieAffix
 * Pour faciliter les imports
 */
public class ZombieAffixRegistry {
    
    /**
     * Obtient l'instance du registre
     */
    public static ZombieAffix.ZombieAffixRegistry getInstance() {
        return ZombieAffix.ZombieAffixRegistry.getInstance();
    }
    
    /**
     * Obtient un affix par son ID
     */
    public static ZombieAffix getAffix(String id) {
        return getInstance().getAffix(id);
    }
    
    /**
     * Obtient tous les affixes
     */
    public static Collection<ZombieAffix> getAllAffixes() {
        return getInstance().getAllAffixes();
    }
    
    /**
     * Tire un affix au sort pour une zone donnée
     */
    public static ZombieAffix rollAffix(int zoneId) {
        return getInstance().rollAffix(zoneId);
    }
    
    /**
     * Obtient le nombre d'affixes
     */
    public static int getAffixCount() {
        return getInstance().getAffixCount();
    }
}
