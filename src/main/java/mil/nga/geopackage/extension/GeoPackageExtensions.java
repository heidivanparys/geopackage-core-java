package mil.nga.geopackage.extension;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import mil.nga.geopackage.GeoPackageCore;
import mil.nga.geopackage.GeoPackageException;
import mil.nga.geopackage.core.contents.ContentsDataType;
import mil.nga.geopackage.db.table.TableInfo;
import mil.nga.geopackage.extension.coverage.CoverageDataCore;
import mil.nga.geopackage.extension.coverage.GriddedCoverageDao;
import mil.nga.geopackage.extension.coverage.GriddedTileDao;
import mil.nga.geopackage.extension.related.RelatedTablesCoreExtension;
import mil.nga.geopackage.features.columns.GeometryColumns;
import mil.nga.geopackage.features.columns.GeometryColumnsDao;
import mil.nga.geopackage.metadata.reference.MetadataReferenceDao;
import mil.nga.geopackage.schema.columns.DataColumnsDao;

/**
 * GeoPackage extension management class for deleting extensions for a table or
 * in a GeoPackage
 * 
 * @author osbornb
 * @since 1.1.8
 */
public class GeoPackageExtensions {

	/**
	 * Logger
	 */
	private static final Logger logger = Logger
			.getLogger(GeoPackageExtensions.class.getName());

	/**
	 * Delete all table extensions for the table within the GeoPackage
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @param table
	 *            table name
	 */
	public static void deleteTableExtensions(GeoPackageCore geoPackage,
			String table) {

		// Handle deleting any extensions with extra tables here
		NGAExtensions.deleteTableExtensions(geoPackage, table);

		deleteRTreeSpatialIndex(geoPackage, table);
		deleteRelatedTables(geoPackage, table);
		deleteGriddedCoverage(geoPackage, table);
		deleteSchema(geoPackage, table);
		deleteMetadata(geoPackage, table);

		delete(geoPackage, table);
	}

	/**
	 * Delete all extensions
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 */
	public static void deleteExtensions(GeoPackageCore geoPackage) {

		// Handle deleting any extensions with extra tables here
		NGAExtensions.deleteExtensions(geoPackage);

		deleteRTreeSpatialIndexExtension(geoPackage);
		deleteRelatedTablesExtension(geoPackage);
		deleteGriddedCoverageExtension(geoPackage);
		deleteSchemaExtension(geoPackage);
		deleteMetadataExtension(geoPackage);
		deleteCrsWktExtension(geoPackage);

		delete(geoPackage);
	}

	/**
	 * Copy all table extensions for the table within the GeoPackage
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @param table
	 *            table name
	 * @param newTable
	 *            new table name
	 * @since 3.2.1
	 */
	public static void copyTableExtensions(GeoPackageCore geoPackage,
			String table, String newTable) {

		copyRTreeSpatialIndex(geoPackage, table, newTable);
		copyRelatedTables(geoPackage, table, newTable);
		copyGriddedCoverage(geoPackage, table, newTable);
		copySchema(geoPackage, table, newTable);
		copyMetadata(geoPackage, table, newTable);

		// Handle copying any extensions with extra tables here
		NGAExtensions.copyTableExtensions(geoPackage, table, newTable);
	}

	/**
	 * Delete the extensions for the table
	 * 
	 * @param geoPackage
	 * @param table
	 */
	private static void delete(GeoPackageCore geoPackage, String table) {

		ExtensionsDao extensionsDao = geoPackage.getExtensionsDao();

		try {
			if (extensionsDao.isTableExists()) {
				extensionsDao.deleteByTableName(table);
			}
		} catch (SQLException e) {
			throw new GeoPackageException(
					"Failed to delete Table extensions. GeoPackage: "
							+ geoPackage.getName() + ", Table: " + table,
					e);
		}
	}

	/**
	 * Delete the extensions
	 * 
	 * @param geoPackage
	 */
	private static void delete(GeoPackageCore geoPackage) {

		ExtensionsDao extensionsDao = geoPackage.getExtensionsDao();

		try {
			if (extensionsDao.isTableExists()) {
				geoPackage.dropTable(extensionsDao.getTableName());
			}
		} catch (SQLException e) {
			throw new GeoPackageException(
					"Failed to delete all extensions. GeoPackage: "
							+ geoPackage.getName(),
					e);
		}

	}

	/**
	 * Delete the RTree Spatial extension for the table
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @param table
	 *            table name
	 * @since 3.2.0
	 */
	public static void deleteRTreeSpatialIndex(GeoPackageCore geoPackage,
			String table) {

		RTreeIndexCoreExtension rTreeIndexExtension = getRTreeIndexExtension(
				geoPackage);
		if (rTreeIndexExtension.has(table)) {
			rTreeIndexExtension.delete(table);
		}

	}

	/**
	 * Delete the RTree Spatial extension
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @since 3.2.0
	 */
	public static void deleteRTreeSpatialIndexExtension(
			GeoPackageCore geoPackage) {

		RTreeIndexCoreExtension rTreeIndexExtension = getRTreeIndexExtension(
				geoPackage);
		if (rTreeIndexExtension.has()) {
			rTreeIndexExtension.deleteAll();
		}

	}

	/**
	 * Copy the RTree Spatial extension for the table
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @param table
	 *            table name
	 * @param newTable
	 *            new table name
	 * @since 3.2.1
	 */
	public static void copyRTreeSpatialIndex(GeoPackageCore geoPackage,
			String table, String newTable) {

		RTreeIndexCoreExtension rTreeIndexExtension = getRTreeIndexExtension(
				geoPackage);
		if (rTreeIndexExtension.has(table)) {
			GeometryColumnsDao geometryColumnsDao = geoPackage
					.getGeometryColumnsDao();
			try {
				GeometryColumns geometryColumns = geometryColumnsDao
						.queryForTableName(newTable);
				if (geometryColumns != null) {
					TableInfo tableInfo = TableInfo
							.info(geoPackage.getDatabase(), newTable);
					if (tableInfo != null) {
						String pk = tableInfo.getPrimaryKey().getName();
						rTreeIndexExtension.create(newTable,
								geometryColumns.getColumnName(), pk);
					}
				}
			} catch (SQLException e) {
				logger.log(
						Level.WARNING, "Failed to create RTree for table: "
								+ newTable + ", copied from table: " + table,
						e);
			}
		}

	}

	/**
	 * Get a RTree Index Extension used only for deletions
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @return RTree index extension
	 */
	private static RTreeIndexCoreExtension getRTreeIndexExtension(
			GeoPackageCore geoPackage) {
		return new RTreeIndexCoreExtension(geoPackage) {
			@Override
			public void createMinYFunction() {
			}

			@Override
			public void createMinXFunction() {
			}

			@Override
			public void createMaxYFunction() {
			}

			@Override
			public void createMaxXFunction() {
			}

			@Override
			public void createIsEmptyFunction() {
			}
		};
	}

	/**
	 * Delete the Related Tables extensions for the table
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @param table
	 *            table name
	 * @since 3.2.0
	 */
	public static void deleteRelatedTables(GeoPackageCore geoPackage,
			String table) {

		RelatedTablesCoreExtension relatedTablesExtension = getRelatedTableExtension(
				geoPackage);
		if (relatedTablesExtension.has()) {
			relatedTablesExtension.removeRelationships(table);
		}

	}

	/**
	 * Delete the Related Tables extension
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @since 3.2.0
	 */
	public static void deleteRelatedTablesExtension(GeoPackageCore geoPackage) {

		RelatedTablesCoreExtension relatedTablesExtension = getRelatedTableExtension(
				geoPackage);
		if (relatedTablesExtension.has()) {
			relatedTablesExtension.removeExtension();
		}

	}

	/**
	 * Copy the Related Tables extensions for the table
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @param table
	 *            table name
	 * @param newTable
	 *            new table name
	 * @since 3.2.1
	 */
	public static void copyRelatedTables(GeoPackageCore geoPackage,
			String table, String newTable) {
		// TODO
	}

	/**
	 * Get a Related Table Extension used only for deletions
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @return Related Table Extension
	 */
	private static RelatedTablesCoreExtension getRelatedTableExtension(
			GeoPackageCore geoPackage) {
		return new RelatedTablesCoreExtension(geoPackage) {
		};
	}

	/**
	 * Delete the Gridded Coverage extensions for the table
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @param table
	 *            table name
	 * @since 3.2.0
	 */
	public static void deleteGriddedCoverage(GeoPackageCore geoPackage,
			String table) {

		if (geoPackage.isTableType(ContentsDataType.GRIDDED_COVERAGE, table)) {

			GriddedTileDao griddedTileDao = geoPackage.getGriddedTileDao();
			GriddedCoverageDao griddedCoverageDao = geoPackage
					.getGriddedCoverageDao();
			ExtensionsDao extensionsDao = geoPackage.getExtensionsDao();

			try {
				if (griddedTileDao.isTableExists()) {
					griddedTileDao.delete(table);
				}
				if (griddedCoverageDao.isTableExists()) {
					griddedCoverageDao.delete(table);
				}
				if (extensionsDao.isTableExists()) {
					extensionsDao.deleteByExtension(
							CoverageDataCore.EXTENSION_NAME, table);
				}
			} catch (SQLException e) {
				throw new GeoPackageException(
						"Failed to delete Table Index. GeoPackage: "
								+ geoPackage.getName() + ", Table: " + table,
						e);
			}
		}

	}

	/**
	 * Delete the Gridded Coverage extension
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @since 3.2.0
	 */
	public static void deleteGriddedCoverageExtension(
			GeoPackageCore geoPackage) {

		List<String> coverageTables = geoPackage
				.getTables(ContentsDataType.GRIDDED_COVERAGE);
		for (String table : coverageTables) {
			geoPackage.deleteTable(table);
		}

		GriddedTileDao griddedTileDao = geoPackage.getGriddedTileDao();
		GriddedCoverageDao griddedCoverageDao = geoPackage
				.getGriddedCoverageDao();
		ExtensionsDao extensionsDao = geoPackage.getExtensionsDao();

		try {
			if (griddedTileDao.isTableExists()) {
				geoPackage.dropTable(griddedTileDao.getTableName());
			}
			if (griddedCoverageDao.isTableExists()) {
				geoPackage.dropTable(griddedCoverageDao.getTableName());
			}
			if (extensionsDao.isTableExists()) {
				extensionsDao
						.deleteByExtension(CoverageDataCore.EXTENSION_NAME);
			}
		} catch (SQLException e) {
			throw new GeoPackageException(
					"Failed to delete Gridded Coverage extension and tables. GeoPackage: "
							+ geoPackage.getName(),
					e);
		}

	}

	/**
	 * Copy the Gridded Coverage extensions for the table
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @param table
	 *            table name
	 * @param newTable
	 *            new table name
	 * @since 3.2.1
	 */
	public static void copyGriddedCoverage(GeoPackageCore geoPackage,
			String table, String newTable) {
		// TODO
	}

	/**
	 * Delete the Schema extensions for the table
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @param table
	 *            table name
	 * @since 3.2.0
	 */
	public static void deleteSchema(GeoPackageCore geoPackage, String table) {

		DataColumnsDao dataColumnsDao = geoPackage.getDataColumnsDao();
		try {
			if (dataColumnsDao.isTableExists()) {
				dataColumnsDao.deleteByTableName(table);
			}
		} catch (SQLException e) {
			throw new GeoPackageException(
					"Failed to delete Schema extension. GeoPackage: "
							+ geoPackage.getName() + ", Table: " + table,
					e);
		}

	}

	/**
	 * Delete the Schema extension
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @since 3.2.0
	 */
	public static void deleteSchemaExtension(GeoPackageCore geoPackage) {

		SchemaExtension schemaExtension = new SchemaExtension(geoPackage);
		if (schemaExtension.has()) {
			schemaExtension.removeExtension();
		}

	}

	/**
	 * Copy the Schema extensions for the table
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @param table
	 *            table name
	 * @param newTable
	 *            new table name
	 * @since 3.2.1
	 */
	public static void copySchema(GeoPackageCore geoPackage, String table,
			String newTable) {
		// TODO
	}

	/**
	 * Delete the Metadata extensions for the table
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @param table
	 *            table name
	 * @since 3.2.0
	 */
	public static void deleteMetadata(GeoPackageCore geoPackage, String table) {

		MetadataReferenceDao metadataReferenceDao = geoPackage
				.getMetadataReferenceDao();
		try {
			if (metadataReferenceDao.isTableExists()) {
				metadataReferenceDao.deleteByTableName(table);
			}
		} catch (SQLException e) {
			throw new GeoPackageException(
					"Failed to delete Metadata extension. GeoPackage: "
							+ geoPackage.getName() + ", Table: " + table,
					e);
		}

	}

	/**
	 * Delete the Metadata extension
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @since 3.2.0
	 */
	public static void deleteMetadataExtension(GeoPackageCore geoPackage) {

		MetadataExtension metadataExtension = new MetadataExtension(geoPackage);
		if (metadataExtension.has()) {
			metadataExtension.removeExtension();
		}

	}

	/**
	 * Copy the Metadata extensions for the table
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @param table
	 *            table name
	 * @param newTable
	 *            new table name
	 * @since 3.2.1
	 */
	public static void copyMetadata(GeoPackageCore geoPackage, String table,
			String newTable) {
		// TODO
	}

	/**
	 * Delete the WKT for Coordinate Reference Systems extension
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @since 3.2.0
	 */
	public static void deleteCrsWktExtension(GeoPackageCore geoPackage) {

		CrsWktExtension crsWktExtension = new CrsWktExtension(geoPackage);
		if (crsWktExtension.has()) {
			crsWktExtension.removeExtension();
		}

	}

}
