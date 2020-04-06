package mil.nga.geopackage.tiles.matrixset;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.j256.ormlite.support.ConnectionSource;

import mil.nga.geopackage.db.GeoPackageDao;

/**
 * Tile Matrix Set Data Access Object
 * 
 * @author osbornb
 */
public class TileMatrixSetDao extends GeoPackageDao<TileMatrixSet, String> {

	/**
	 * Constructor, required by ORMLite
	 * 
	 * @param connectionSource
	 *            connection source
	 * @param dataClass
	 *            data class
	 * @throws SQLException
	 *             upon failure
	 */
	public TileMatrixSetDao(ConnectionSource connectionSource,
			Class<TileMatrixSet> dataClass) throws SQLException {
		super(connectionSource, dataClass);
	}

	/**
	 * Get all the tile table names
	 * 
	 * @return tile tables
	 * @throws SQLException
	 *             upon failure
	 */
	public List<String> getTileTables() throws SQLException {

		List<String> tableNames = new ArrayList<String>();

		List<TileMatrixSet> tileMatrixSets = queryForAll();
		for (TileMatrixSet tileMatrixSet : tileMatrixSets) {
			tableNames.add(tileMatrixSet.getTableName());
		}

		return tableNames;
	}

}
