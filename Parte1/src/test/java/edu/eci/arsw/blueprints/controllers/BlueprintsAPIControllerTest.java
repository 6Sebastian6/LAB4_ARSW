package edu.eci.arsw.blueprints.controllers;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import edu.eci.arsw.blueprints.services.BlueprintsServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BlueprintsAPIController.class)
class BlueprintsAPIControllerTest {

    private static final String BASE = "/api/v1/blueprints";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private BlueprintsServices services;

    @Test
    void getAllReturns200WithApiResponseEnvelope() throws Exception {
        when(services.getAllBlueprints()).thenReturn(Set.of(house()));

        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].author").value("john"))
                .andExpect(jsonPath("$.data[0].points.length()").value(2));
    }

    @Test
    void unknownAuthorReturns404() throws Exception {
        when(services.getBlueprintsByAuthor("nobody"))
                .thenThrow(new BlueprintNotFoundException("No blueprints for author: nobody"));

        mvc.perform(get(BASE + "/nobody"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("No blueprints for author: nobody"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void getByAuthorAndNameReturns200() throws Exception {
        when(services.getBlueprint("john", "house")).thenReturn(house());

        mvc.perform(get(BASE + "/john/house"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("house"));
    }

    @Test
    void createReturns201WithCreatedBlueprint() throws Exception {
        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"author":"john","name":"kitchen","points":[{"x":1,"y":1},{"x":2,"y":2}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.name").value("kitchen"))
                .andExpect(jsonPath("$.data.points.length()").value(2));

        verify(services).addNewBlueprint(any(Blueprint.class));
    }

    @Test
    void createWithBlankNameReturns400() throws Exception {
        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"author":"john","name":"","points":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("name: must not be blank"));
    }

    @Test
    void createDuplicateReturns400() throws Exception {
        doThrow(new BlueprintPersistenceException("Blueprint already exists: john:house"))
                .when(services).addNewBlueprint(any(Blueprint.class));

        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"author":"john","name":"house","points":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Blueprint already exists: john:house"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void addPointReturns202WithPoint() throws Exception {
        mvc.perform(put(BASE + "/john/house/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"x":3,"y":3}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value(202))
                .andExpect(jsonPath("$.data.x").value(3))
                .andExpect(jsonPath("$.data.y").value(3));

        verify(services).addPoint("john", "house", 3, 3);
    }

    @Test
    void addPointToMissingBlueprintReturns404() throws Exception {
        doThrow(new BlueprintNotFoundException("Blueprint not found: john/none"))
                .when(services).addPoint("john", "none", 1, 1);

        mvc.perform(put(BASE + "/john/none/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"x":1,"y":1}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    private static Blueprint house() {
        return new Blueprint("john", "house", List.of(new Point(0, 0), new Point(10, 10)));
    }
}
