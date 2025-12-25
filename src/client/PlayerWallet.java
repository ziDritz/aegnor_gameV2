package client;

import game.world.World;
import kernel.Config;

public class PlayerWallet {

    private final Player owner;
    private long kamas;

    public PlayerWallet(Player owner, long kamas) {
        this.owner = owner;
        this.kamas = kamas;
    }

    public long getKamas() {
        return kamas;
    }

    public void setKamas(long amount) {
        if(amount < 0) {
            owner.handleInsufficientFunds(amount);
        }
        else{
            this.kamas = amount;
        }
    }

    public void addKamas(long l) {
        // Si retrait d'argent
        if(l < 0 ){
            // Si le joueur n'avait pas l'argent qu'il a essayer de se faire retirer : USE FAILLE BAN
            if( ( getKamas() + l) < 0 ) {
                owner.handleInsufficientFunds(l);
                setKamas(0);
            }
            else {
                setKamas(getKamas() + l);
            }
        }
        // Si ajout d'argent
        else {
            setKamas(getKamas() + l);
        }
    }
}
