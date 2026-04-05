package me.anduria.staffsystem.storage;

import me.anduria.staffsystem.report.Report;

/**
 * Rozhrani pro uloziste reportu.
 * Umoznuje snadne pridavani novych backendu (SQLite, MongoDB atd.).
 */
public interface StorageProvider {

    /**
     * Inicializuje uloziste (vytvori tabulku/soubor, pripoji se k DB atd.).
     * Vola se jednou pri spusteni pluginu.
     */
    void initialize();

    /**
     * Ulozi report do uloziste.
     *
     * @param report report k ulozeni
     */
    void saveReport(Report report);
}