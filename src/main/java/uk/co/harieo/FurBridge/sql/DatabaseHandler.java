package uk.co.harieo.FurBridge.sql;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface DatabaseHandler {

	List<InfoTable> getReferencedTables();

	/**
	 * Verifies that all tables contained in {@link #getReferencedTables()} exist in the database. Default
	 * implementation cycles through each instance of {@link InfoTable} and calls {@link InfoTable#createTable()} while
	 * checking the outcome. If any errors occur, the entire cycle will break and the system should be considered
	 * malfunctioning.
	 *
	 * @return true if the call was a success or false is an error occurred
	 */
	default CompletableFuture<Boolean> verifyTables() {
		return CompletableFuture.supplyAsync(() -> {
			boolean error = false;
			try {
				for (InfoTable table : getReferencedTables()) {
					if (!table.createTable().get()) {
						error = true;
						break;
					}
				}
			} catch (ExecutionException | InterruptedException e) {
				e.printStackTrace();
				error = true;
			}

			return !error; // This will return true on success and false on error
		});
	}

}
