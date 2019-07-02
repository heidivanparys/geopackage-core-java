package mil.nga.geopackage.features;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mil.nga.geopackage.GeoPackageCore;
import mil.nga.geopackage.GeoPackageException;
import mil.nga.geopackage.db.DateConverter;
import mil.nga.geopackage.io.GeoPackageIOUtils;
import mil.nga.oapi.features.json.Collection;
import mil.nga.oapi.features.json.Crs;
import mil.nga.oapi.features.json.FeatureCollection;
import mil.nga.oapi.features.json.FeaturesConverter;
import mil.nga.oapi.features.json.Link;
import mil.nga.sf.geojson.Feature;
import mil.nga.sf.proj.Projection;
import mil.nga.sf.proj.ProjectionConstants;

/**
 * OGC API Features Generator
 * 
 * @author osbornb
 */
public abstract class OAPIFeatureCoreGenerator extends FeatureCoreGenerator {

	/**
	 * Logger
	 */
	private static final Logger LOGGER = Logger
			.getLogger(OAPIFeatureCoreGenerator.class.getName());

	/**
	 * Limit pattern
	 */
	protected static final Pattern LIMIT_PATTERN = Pattern
			.compile("limit=\\d+");

	/**
	 * Base server url
	 */
	protected final String server;

	/**
	 * Identifier (name) of a specific collection
	 */
	protected final String name;

	/**
	 * The optional limit parameter limits the number of items that are
	 * presented in the response document.
	 */
	protected Integer limit = null;

	/**
	 * Either a date-time or a period string that adheres to RFC 3339
	 */
	protected String time = null;

	/**
	 * Time period string that adheres to RFC 3339
	 */
	protected String period = null;

	/**
	 * Total limit of number of items to request
	 */
	protected Integer totalLimit = null;

	/**
	 * Download attempts per tile
	 */
	protected int downloadAttempts = 1;

	/**
	 * Constructor
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @param tableName
	 *            table name
	 * @param server
	 *            server url
	 * @param name
	 *            collection identifier
	 */
	public OAPIFeatureCoreGenerator(GeoPackageCore geoPackage, String tableName,
			String server, String name) {
		super(geoPackage, tableName);
		this.server = server;
		this.name = name;
	}

	/**
	 * Get the server
	 * 
	 * @return server
	 */
	public String getServer() {
		return server;
	}

	/**
	 * Get the name
	 * 
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the limit
	 * 
	 * @return limit
	 */
	public Integer getLimit() {
		return limit;
	}

	/**
	 * Set the limit
	 * 
	 * @param limit
	 *            limit
	 */
	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	/**
	 * Get the time
	 * 
	 * @return time
	 */
	public String getTime() {
		return time;
	}

	/**
	 * Set the time
	 * 
	 * @param time
	 *            time
	 */
	public void setTime(String time) {
		this.time = time;
	}

	/**
	 * Set the time
	 * 
	 * @param time
	 *            time
	 */
	public void setTime(Date time) {
		if (time != null) {
			DateConverter dateConverter = DateConverter
					.dateConverter(DateConverter.DATETIME_FORMAT2);
			this.time = dateConverter.stringValue(time);
		} else {
			this.time = null;
		}
	}

	/**
	 * Get the time period
	 * 
	 * @return period
	 */
	public String getPeriod() {
		return period;
	}

	/**
	 * Set the time period
	 * 
	 * @param period
	 *            period
	 */
	public void setPeriod(String period) {
		this.period = period;
	}

	/**
	 * Set the time period
	 * 
	 * @param period
	 *            period
	 */
	public void setPeriod(Date period) {
		if (period != null) {
			DateConverter dateConverter = DateConverter
					.dateConverter(DateConverter.DATETIME_FORMAT2);
			this.period = dateConverter.stringValue(period);
		} else {
			this.period = null;
		}
	}

	/**
	 * Get the total limit
	 * 
	 * @return total limit
	 */
	public Integer getTotalLimit() {
		return totalLimit;
	}

	/**
	 * Set the total limit
	 * 
	 * @param totalLimit
	 *            total limit
	 */
	public void setTotalLimit(Integer totalLimit) {
		this.totalLimit = totalLimit;
	}

	/**
	 * Get the number of download attempts
	 * 
	 * @return download attempts
	 */
	public int getDownloadAttempts() {
		return downloadAttempts;
	}

	/**
	 * Set the number of download attempts
	 * 
	 * @param downloadAttempts
	 *            download attempts
	 */
	public void setDownloadAttempts(int downloadAttempts) {
		this.downloadAttempts = downloadAttempts;
	}

	/**
	 * Create the feature
	 * 
	 * @param feature
	 *            feature
	 * @throws SQLException
	 *             upon error
	 */
	protected void createFeature(Feature feature) throws SQLException {
		createFeature(feature.getSimpleGeometry(), feature.getProperties());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int generateFeatures() throws SQLException {

		String url = buildCollectionRequestUrl();

		Map<String, Map<String, Projection>> projections = getProjections(url);
		if (getProjection(projections, projection) == null) {
			LOGGER.log(Level.WARNING,
					"The projection is not advertised by the server. Authority: "
							+ projection.getAuthority() + ", Code: "
							+ projection.getCode());
		}

		StringBuilder urlBuilder = new StringBuilder(url);

		urlBuilder.append("/items");

		boolean params = false;

		if (time != null) {
			if (params) {
				urlBuilder.append("&");
			} else {
				urlBuilder.append("?");
				params = true;
			}
			urlBuilder.append("time=");
			urlBuilder.append(time);
			if (period != null) {
				urlBuilder.append("/");
				urlBuilder.append(period);
			}
		}

		if (boundingBox != null) {
			if (params) {
				urlBuilder.append("&");
			} else {
				urlBuilder.append("?");
				params = true;
			}
			urlBuilder.append("bbox=");
			urlBuilder.append(boundingBox.getMinLongitude());
			urlBuilder.append(",");
			urlBuilder.append(boundingBox.getMinLatitude());
			urlBuilder.append(",");
			urlBuilder.append(boundingBox.getMaxLongitude());
			urlBuilder.append(",");
			urlBuilder.append(boundingBox.getMaxLatitude());
		}

		String urlValue = urlBuilder.toString();

		return generateFeatures(urlValue, 0);
	}

	/**
	 * Build the collection request URL
	 * 
	 * @return url
	 */
	protected String buildCollectionRequestUrl() {

		StringBuilder urlBuilder = new StringBuilder(server);

		if (!server.endsWith("/")) {
			urlBuilder.append("/");
		}

		urlBuilder.append("collections/");
		urlBuilder.append(name);

		return urlBuilder.toString();
	}

	/**
	 * Get the supported projections
	 * 
	 * @return map of orgs and projections
	 */
	public Map<String, Map<String, Projection>> getProjections() {
		return getProjections(buildCollectionRequestUrl());
	}

	/**
	 * Get the supported projections
	 * 
	 * @param url
	 *            URL
	 * @return map of orgs and projections
	 */
	public Map<String, Map<String, Projection>> getProjections(String url) {
		return getProjections(collectionRequest(url));
	}

	/**
	 * Get the supported projections
	 * 
	 * @param collection
	 *            collection
	 * @return map of orgs and projections
	 */
	public Map<String, Map<String, Projection>> getProjections(
			Collection collection) {

		Map<String, Map<String, Projection>> projections = new HashMap<>();

		if (collection != null) {

			for (String crs : collection.getCrs()) {

				Crs crsValue = new Crs(crs);
				if (crsValue.isValid()) {
					addProjection(projections, crsValue.getAuthority(),
							crsValue.getCode());
				}

			}

		}

		if (projections.isEmpty()) {
			addProjection(projections, ProjectionConstants.AUTHORITY_OGC,
					ProjectionConstants.OGC_CRS84);
			addProjection(projections, ProjectionConstants.AUTHORITY_EPSG,
					String.valueOf(
							ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM));
		} else if (getProjection(projections, ProjectionConstants.AUTHORITY_OGC,
				ProjectionConstants.OGC_CRS84) != null) {
			addProjection(projections, ProjectionConstants.AUTHORITY_EPSG,
					String.valueOf(
							ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM));
		}

		return projections;
	}

	/**
	 * Collection request
	 * 
	 * @return collection
	 */
	public Collection collectionRequest() {
		return collectionRequest(buildCollectionRequestUrl());
	}

	/**
	 * Collection request for the provided URL
	 * 
	 * @param url
	 *            url value
	 * @return collection
	 */
	protected Collection collectionRequest(String url) {

		Collection collection = null;

		String collectionValue = null;
		try {
			collectionValue = urlRequest(url);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING,
					"Failed to request the collection. url: " + url, e);
		}

		if (collectionValue != null) {

			try {
				collection = FeaturesConverter.toCollection(collectionValue);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING,
						"Failed to translate collection. url: " + url, e);
			}

		}

		return collection;
	}

	/**
	 * Generate features
	 * 
	 * @param urlString
	 *            URL
	 * @param currentCount
	 *            current count
	 * @return current result count
	 * @throws SQLException
	 *             upon failure
	 */
	public int generateFeatures(String urlString, int currentCount)
			throws SQLException {

		StringBuilder urlBuilder = new StringBuilder(urlString);

		int paramIndex = urlString.lastIndexOf("?");
		boolean params = paramIndex >= 0 && paramIndex + 1 < urlString.length();

		Integer requestLimit = limit;
		if (totalLimit != null && totalLimit
				- currentCount < (requestLimit != null ? requestLimit
						: FeatureCollection.LIMIT_DEFAULT)) {
			requestLimit = totalLimit - currentCount;
		}
		if (requestLimit != null) {
			Matcher matcher = LIMIT_PATTERN.matcher(urlBuilder.toString());
			if (matcher.find()) {
				urlBuilder = new StringBuilder(
						matcher.replaceFirst("limit=" + requestLimit));
			} else {
				if (params) {
					urlBuilder.append("&");
				} else {
					urlBuilder.append("?");
					params = true;
				}
				urlBuilder.append("limit=");
				urlBuilder.append(requestLimit);
			}
		}

		String features = urlRequest(urlBuilder.toString());

		if (features != null) {
			FeatureCollection featureCollection = createFeatures(features);

			Integer numberReturned = featureCollection.getNumberReturned();
			if (numberReturned != null) {
				currentCount += numberReturned;
			}

			List<Link> nextLinks = featureCollection.getRelationLinks()
					.get(FeatureCollection.LINK_RELATION_NEXT);
			if (nextLinks != null) {
				for (Link nextLink : nextLinks) {
					if (totalLimit != null && totalLimit <= currentCount) {
						break;
					}
					currentCount = generateFeatures(nextLink.getHref(),
							currentCount);
				}
			}
		}

		return currentCount;
	}

	/**
	 * URL request
	 * 
	 * @param urlValue
	 *            URL value
	 * @return response string
	 */
	protected String urlRequest(String urlValue) {

		String response = null;

		URL url;
		try {
			url = new URL(urlValue);
		} catch (MalformedURLException e) {
			throw new GeoPackageException("Failed request. URL: " + urlValue,
					e);
		}

		int attempt = 1;
		while (true) {
			try {
				response = urlRequest(urlValue, url);
				break;
			} catch (Exception e) {
				if (attempt < downloadAttempts) {
					LOGGER.log(Level.WARNING,
							"Failed to download features after attempt "
									+ attempt + " of " + downloadAttempts
									+ ". URL: " + urlValue,
							e);
					attempt++;
				} else {
					throw new GeoPackageException(
							"Failed to download features after "
									+ downloadAttempts + " attempts. URL: "
									+ urlValue,
							e);
				}
			}
		}

		return response;
	}

	/**
	 * Perform a URL request
	 * 
	 * @param urlValue
	 *            URL string value
	 * @param url
	 *            URL
	 * @return features response
	 */
	protected String urlRequest(String urlValue, URL url) {

		String response = null;

		HttpURLConnection connection = null;
		try {
			LOGGER.log(Level.INFO, urlValue);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestProperty("Accept",
					"application/json,application/geo+json");
			connection.connect();

			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_MOVED_PERM
					|| responseCode == HttpURLConnection.HTTP_MOVED_TEMP
					|| responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
				String redirect = connection.getHeaderField("Location");
				connection.disconnect();
				url = new URL(redirect);
				connection = (HttpURLConnection) url.openConnection();
				connection.connect();
			}

			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				throw new GeoPackageException("Failed request. URL: " + urlValue
						+ ", Response Code: " + connection.getResponseCode()
						+ ", Response Message: "
						+ connection.getResponseMessage());
			}

			InputStream responseStream = connection.getInputStream();
			response = GeoPackageIOUtils.streamString(responseStream);

		} catch (IOException e) {
			throw new GeoPackageException("Failed request. URL: " + urlValue,
					e);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}

		return response;
	}

	/**
	 * Create features in the feature DAO from the features value
	 * 
	 * @param features
	 *            features json
	 * @return feature collection
	 * @throws SQLException
	 *             upon error
	 */
	protected FeatureCollection createFeatures(String features)
			throws SQLException {

		FeatureCollection featureCollection = FeaturesConverter
				.toFeatureCollection(features);

		createFeatures(featureCollection);

		return featureCollection;
	}

	/**
	 * Create features from the feature collection
	 * 
	 * @param featureCollection
	 *            feature collection
	 */
	protected void createFeatures(FeatureCollection featureCollection) {

		int count = 0;

		geoPackage.beginTransaction();
		try {

			for (Feature feature : featureCollection.getFeatureCollection()
					.getFeatures()) {
				try {
					createFeature(feature);
					count++;
				} catch (Exception e) {
					LOGGER.log(Level.WARNING,
							"Failed to create feature: " + feature.getId(), e);
				}

				if (count > 0 && count % transactionLimit == 0) {
					geoPackage.commit();
				}

			}

		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to create features", e);
			geoPackage.failTransaction();
		} finally {
			geoPackage.endTransaction();
		}

	}

}
