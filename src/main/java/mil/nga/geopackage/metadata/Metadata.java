package mil.nga.geopackage.metadata;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Contains metadata in MIME encodings structured in accordance with any
 * authoritative metadata specification
 * 
 * @author osbornb
 */
@DatabaseTable(tableName = "gpkg_metadata", daoClass = MetadataDao.class)
public class Metadata {

	/**
	 * Table name
	 */
	public static final String TABLE_NAME = "gpkg_metadata";

	/**
	 * id field name
	 */
	public static final String COLUMN_ID = "id";

	/**
	 * scope field name
	 */
	public static final String COLUMN_SCOPE = "md_scope";

	/**
	 * standardUri field name
	 */
	public static final String COLUMN_STANDARD_URI = "md_standard_uri";

	/**
	 * mimeType field name
	 */
	public static final String COLUMN_MIME_TYPE = "mime_type";

	/**
	 * metadata field name
	 */
	public static final String COLUMN_METADATA = "metadata";

	/**
	 * Metadata primary key
	 */
	@DatabaseField(columnName = COLUMN_ID, generatedId = true, canBeNull = false)
	private long id;

	/**
	 * Case sensitive name of the data scope to which this metadata applies; see
	 * Metadata Scopes below
	 */
	@DatabaseField(columnName = COLUMN_SCOPE, canBeNull = false, defaultValue = "dataset")
	private String scope;

	/**
	 * URI reference to the metadata structure definition authority
	 */
	@DatabaseField(columnName = COLUMN_STANDARD_URI, canBeNull = false)
	private String standardUri;

	/**
	 * MIME encoding of metadata
	 */
	@DatabaseField(columnName = COLUMN_MIME_TYPE, canBeNull = false, defaultValue = "text/xml")
	private String mimeType;

	/**
	 * metadata
	 */
	@DatabaseField(columnName = COLUMN_METADATA, canBeNull = false, defaultValue = "")
	private String metadata;

	/**
	 * Default Constructor
	 */
	public Metadata() {

	}

	/**
	 * Copy Constructor
	 * 
	 * @param meta
	 *            metadata to copy
	 * @since 1.3.0
	 */
	public Metadata(Metadata meta) {
		id = meta.id;
		scope = meta.scope;
		standardUri = meta.standardUri;
		mimeType = meta.mimeType;
		metadata = meta.metadata;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public MetadataScopeType getMetadataScope() {
		return MetadataScopeType.fromName(scope);
	}

	public void setMetadataScope(MetadataScopeType metadataScope) {
		this.scope = metadataScope.getName();
	}

	public String getMetadataScopeString() { return scope; }

	public void setMetadataScope(String metadataScope) { this.scope = metadataScope; }

	public String getStandardUri() {
		return standardUri;
	}

	public void setStandardUri(String standardUri) {
		this.standardUri = standardUri;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}
}
