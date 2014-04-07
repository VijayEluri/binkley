package hm.binkley.dao;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

/**
 * A very simple DAO wrapper for using Spring transactions and JDBC template, designed for lambda
 * use.  Example:
 *
 * <pre>
 * SimpleDAO&lt;String&gt; someColumn = new SimpleDAO(transactionManager);
 * String columnValue = someColumn.dao(
 *        (jdbcTemplate, status) -&gt; jdbcTemplate.queryForObject("sql here", String.class);</pre>
 *
 * @author <a href="binkley@alumni.rice.edu">B. K. Oxley (binkley)</a>
 */
public class SimpleDAO {
    private final DataSourceTransactionManager transactionManager;

    /**
     * Constructs a new {@code SimpleDAO} with the given data source <var>transactionManager</var>.
     *
     * @param transactionManager the transaction manager, never missing
     */
    @Inject
    public SimpleDAO(@Nonnull final DataSourceTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * Access to the underlying transaction manager.
     *
     * @return the transaction manager, never missing
     */
    @Nonnull
    public DataSourceTransactionManager getTransactionManager() {
        return transactionManager;
    }

    /**
     * Runs the given <var>dao</var> within Spring transaction.
     *
     * @param dao the dao callback, never missing
     * @param <T> the return type of the callback
     *
     * @return the callback result
     */
    public final <T> T dao(@Nonnull final Dao<T> dao) {
        return dao.using(transactionManager);
    }

    /**
     * Runs the given <var>dao</var> within Spring transaction, requiring 0 or 1 results.
     *
     * @param dao the dao callback, never missing
     * @param wrongSize the exception factory for more than 0 or 1 results, never missing
     * @param <T> the return type of the callback
     *
     * @return an optional of the return
     *
     * @throws DataAccessException if the results are the wrong size
     */
    public final <T> Optional<T> daoMaybe(@Nonnull final Dao<List<T>> dao,
            @Nonnull final WrongSize wrongSize) {
        final List<T> result = dao(dao);
        final int size = result.size();
        switch (size) {
        case 0:
            return Optional.empty();
        case 1:
            return Optional.of(result.get(0));
        default:
            throw wrongSize.fail(size);
        }
    }

    /**
     * The functional interface for {@link #dao(Dao)}.
     *
     * @param <T> the callback return type
     */
    @FunctionalInterface
    public interface Dao<T> {
        /**
         * Manages the JDBC callback, wrapping it in a Spring transaction.  The JDBC template passed
         * to the callback shares the data source of the transaction manager, and executes within a
         * transaction template.
         *
         * @param transactionManager the transaction manager, never missing
         *
         * @return the callback result
         */
        default T using(@Nonnull final DataSourceTransactionManager transactionManager) {
            return new TransactionTemplate(transactionManager).execute(
                    status -> with(new JdbcTemplate(transactionManager.getDataSource()), status));
        }

        /**
         * Executes the callback, passing in the given <var>jdbcTemplate</var> and transaction
         * <var>status</var>.  This is typically implemented as a lambda.
         *
         * @param jdbcTemplate the JDBC template, never missing
         * @param status the transaction status, never missing
         *
         * @return the callback result
         */
        T with(@Nonnull final JdbcTemplate jdbcTemplate, @Nonnull final TransactionStatus status);
    }

    @FunctionalInterface
    public interface WrongSize {
        DataAccessException fail(final int size);
    }
}
