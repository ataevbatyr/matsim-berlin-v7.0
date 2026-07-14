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

	private static final String ROAD_PRICING_SHAPEFILE = "input/v7.0/ring_zone-1pct/ring.shp";

	public static void main(String[] args) {
		MATSimApplication.execute(BerlinScenarioRoadPricing.class, args);
	}

	@Override
	protected Config prepareConfig(Config config) {
		super.prepareConfig(config);

		config.controller().setLastIteration(5);
		config.controller().setOutputDirectory("output/berlin-road-pricing-5-iterations");
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
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

			if (!fromNodeInside || !toNodeInside) continue;

			RoadPricingUtils.addLink(roadPricingScheme, link.getId());
		}

		RoadPricingUtils.createAndAddGeneralCost(roadPricingScheme, 0, 36 * 3600, 0.00015);
		RoadPricingUtils.setType(roadPricingScheme, RoadPricingSchemeImpl.TOLL_TYPE_DISTANCE);
	}

	@Override
	protected void prepareControler(Controler controler) {
		super.prepareControler(controler);
		controler.addOverridingModule(new RoadPricingModule());
	}
}
