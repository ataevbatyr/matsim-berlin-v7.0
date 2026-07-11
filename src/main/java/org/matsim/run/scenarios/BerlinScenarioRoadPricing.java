package org.matsim.run.scenarios;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.roadpricing.RoadPricingModule;
import org.matsim.contrib.roadpricing.RoadPricingSchemeImpl;
import org.matsim.contrib.roadpricing.RoadPricingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.OpenBerlinScenario;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.util.List;

public class BerlinScenarioRoadPricing extends OpenBerlinScenario {

	private static final String ROAD_PRICING_SHAPEFILE = "input/v6.4/road-pricing/berlin_road_pricing_area.shp";

	public static void main(String[] args) {
		MATSimApplication.execute(BerlinScenarioRoadPricing.class, args);
	}

	@Override
	protected Config prepareConfig(Config config) {
		// Apply the OpenBerlinScenarioConfiguration
		super.prepareConfig(config);

		config.controller().setLastIteration(0);
		config.controller().setOutputDirectory("output/berlin-road-pricing");
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		// Apply the complete OpenBerlinScenario preparation
		super.prepareScenario(scenario);
		RoadPricingSchemeImpl roadPricingScheme = RoadPricingUtils.addOrGetMutableRoadPricingScheme(scenario);

		List<Geometry> geometries = ShpGeometryUtils.loadGeometries(IOUtils.getFileUrl(ROAD_PRICING_SHAPEFILE));

		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getId().toString().startsWith("pt_")) {
				continue;
			}

			Node fromNode = link.getFromNode();
			Node toNode = link.getToNode();

			boolean fromNodeInside = ShpGeometryUtils.isCoordInGeometries(fromNode.getCoord(), geometries);
			boolean toNodeInside = ShpGeometryUtils.isCoordInGeometries(toNode.getCoord(), geometries);

			// Only add links whose from-node and to-node are both inside the road-pricing geometry
			if (!fromNodeInside || !toNodeInside) continue;

			RoadPricingUtils.addLink(roadPricingScheme, link.getId());
		}

		// Charge 1 monetary unit throughout the simulated day.
		RoadPricingUtils.createAndAddGeneralCost(roadPricingScheme, 0, 36 * 3600, 1.0);
		RoadPricingUtils.setType(roadPricingScheme, RoadPricingSchemeImpl.TOLL_TYPE_LINK);
	}

	@Override
	protected void prepareControler(Controler controler) {
		super.prepareControler(controler);
		controler.addOverridingModule(new RoadPricingModule());
	}
}
