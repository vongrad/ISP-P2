/**
 * This class implements the logic behind the BDD for the n-queens problem
 * You should implement all the missing methods
 * 
 * @author Stavros Amanatidis
 *
 */
import java.util.*;

import net.sf.javabdd.*;

public class QueensLogic {

    private int x = 0;
    private int y = 0;
    private int size = 0;
    private int[][] board;

    private BDDFactory factory;

    private BDD bdd;

    private BDD TRUE;
    private BDD FALSE;

    public QueensLogic() {
       factory = JFactory.init(2000000,200000);
       TRUE = factory.one();
       FALSE = factory.zero();
    }

    public void initializeGame(int size) {
        this.size = size;

        this.x = size;
        this.y = size;

        this.board = new int[x][y];

        this.factory.setVarNum(size * size);

        bdd = TRUE;

        initializeBDD();
    }

    /**
     * Initialize BDD for N-queen problem
     */
    public void initializeBDD() {
        for(int x = 0; x < size; x++) {
            for(int y = 0; y < size; y++) {
                bdd = bdd.and(getSquareRestriction(x, y));
            }
        }
        bdd = bdd.and(getNQueenRule());
    }

    /**
     * Get N-queen restricted BDD
     * Restrict each column to have exactly one queen
     * @return - restricted sub BDD
     */
    public BDD getNQueenRule() {
        BDD subBDD = TRUE;

        for (int x = 0; x < size; x++) {

            BDD row = FALSE;

            for (int y = 0; y < size; y++) {
                row = row.or(factory.ithVar(variableIndex(x, y)));
            }
            subBDD = subBDD.and(row);
        }

        return subBDD;
    }

    /**
     * Get square restricted BDD
     * Restrict a square on the board in all horizontal, vertical, slash-diagonal and backslash-diagonal lines
     * @param x - queen's X
     * @param y - queen's Y
     * @return - restricted sub BDD
     */
    public BDD getSquareRestriction(int x, int y) {
        BDD subBDD = FALSE;
        BDD unoccupied = TRUE;

        unoccupied = unoccupied.and(getVerticalRestriction(x, y));
        unoccupied = unoccupied.and(getHorizontalRestriction(x, y));
        unoccupied = unoccupied.and(getSlashDiagonalRestriction(x, y));
        unoccupied = unoccupied.and(getBackslashDiagonalRestriction(x, y));

        subBDD = subBDD.or(factory.nithVar(variableIndex(x, y)));
        subBDD = subBDD.or(factory.ithVar(variableIndex(x, y)).and(unoccupied));

        return subBDD;
    }

    /**
     * Get column restricted BDD
     * Restrict all squares in a vertical line except the one for queen's position
     * @param placeX - queen's X
     * @param placeY - queen's Y
     * @return - restricted sub BDD
     */
    public BDD getVerticalRestriction(int placeX, int placeY) {

        BDD unoccupied = TRUE;

        for(int y = 0; y < size; y++) {
            if(y != placeY) {
                unoccupied = unoccupied.and(factory.nithVar(variableIndex(placeX, y)));
            }
        }
        return unoccupied;
    }

    /**
     * Get row restricted BDD
     * Restrict all squares in a horizontal line except the one for queen's position
     * @param placeX - queen's X
     * @param placeY - queen's Y
     * @return - restricted sub BDD
     */
    public BDD getHorizontalRestriction(int placeX, int placeY) {
        BDD unoccupied = TRUE;

        for(int x = 0; x < size; x++) {
            if(x != placeX) {
                unoccupied = unoccupied.and(factory.nithVar(variableIndex(x, placeY)));
            }
        }
        return unoccupied;
    }

    /**
     * Get slash diagonal restricted BDD
     * Restrict all squares in a slash diagonal line except the one for queen's position
     * @param placeX - queen's X
     * @param placeY - queen's Y
     * @return - restricted sub BDD
     */
    public BDD getSlashDiagonalRestriction(int placeX, int placeY) {

        int normalize = Math.min(placeX, placeY);

        int x = placeX - normalize;
        int y = placeY - normalize;

        BDD unoccupied = TRUE;

        while (x < size && y < size) {
            if(x != placeX && y != placeY) {
                unoccupied = unoccupied.and(factory.nithVar(variableIndex(x, y)));
            }
            x++;
            y++;
        }
        return unoccupied;
    }

    /**
     * Get backslash diagonal restricted BDD
     * Restrict all squares in a backslash diagonal line except the one for queen's position
     * @param placeX - queen's X
     * @param placeY - queen's Y
     * @return - restricted sub BDD
     */
    public BDD getBackslashDiagonalRestriction(int placeX, int placeY) {
        int normalize = Math.min(size - 1 - placeX, placeY);

        int x = placeX + normalize;
        int y = placeY - normalize;

        BDD unoccupied = TRUE;

        while (x >= 0 && y < size) {
            if(x != placeX && y != placeY) {
                unoccupied = unoccupied.and(factory.nithVar(variableIndex(x, y)));
            }
            x--;
            y++;
        }
        return unoccupied;
    }

    /**
     * Get game board
     * @return int[][] board
     */
    public int[][] getGameBoard() {
        return board;
    }

    /**
     * Insert queen into a specific square on the board
     * @param x - queen's X
     * @param y - queen's Y
     * @return - position valid
     */
    public boolean insertQueen(int x, int y) {

        if (board[x][y] == -1 || board[x][y] == 1) {
            return true;
        }
        
        board[x][y] = 1;

        bdd = bdd.restrict(factory.ithVar(variableIndex(x, y)));

        updateGameBoard();

        if(shouldAutocomplete()) {
            autocomplete();
        }
      
        return true;
    }

    /**
     * Mark the invalid squares (those that would lead to unsatisfiable solution)
     */
    public void updateGameBoard() {

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                BDD bdd = this.bdd.restrict(factory.ithVar(variableIndex(x, y)));

                if (bdd.isZero()) {
                    board[x][y] = -1;
                }
            }
        }
    }

    /**
     * Check whether the game should autocomplete
     * @return boolean
     */
    public boolean shouldAutocomplete() {
        int queensPlaced = 0;
        int emptySquares = 0;

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if(board[x][y] == 1) {
                    queensPlaced++;
                }
                if(board[x][y] == 0) {
                    emptySquares++;
                }
            }
        }
        return size - queensPlaced == emptySquares;
    }

    /**
     * Automatically finish the game
     */
    public void autocomplete() {
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if(board[x][y] == 0) {
                    board[x][y] = 1;
                    bdd = bdd.restrict(factory.ithVar(variableIndex(x, y)));
                }
            }
        }
    }

    /**
     * Get index of the variable in the BDD
     * @param x - row
     * @param y - column
     * @return - int variable index
     */
    private int variableIndex(int x, int y) {
        return (this.size * y) + x;
    }
}
