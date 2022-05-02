package com.luv2code.springmvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luv2code.springmvc.models.CollegeStudent;
import com.luv2code.springmvc.models.MathGrade;
import com.luv2code.springmvc.repository.HistoryGradesDao;
import com.luv2code.springmvc.repository.MathGradesDao;
import com.luv2code.springmvc.repository.ScienceGradesDao;
import com.luv2code.springmvc.repository.StudentDao;
import com.luv2code.springmvc.service.StudentAndGradeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestPropertySource("/application-test.properties")
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
public class GradebookControllerTest {

    private static MockHttpServletRequest request;

    @PersistenceContext
    private EntityManager entityManager;

    @Mock
    StudentAndGradeService studentCreateServiceMock;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StudentDao studentDao;

    @Autowired
    private MathGradesDao mathGradeDao;

    @Autowired
    private ScienceGradesDao scienceGradeDao;

    @Autowired
    private HistoryGradesDao historyGradeDao;

    @Autowired
    private StudentAndGradeService studentService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private CollegeStudent student;

    @Value("${sql.script.create.student}")
    private String sqlAddStudent;

    @Value("${sql.script.create.math.grade}")
    private String sqlAddMathGrade;

    @Value("${sql.script.create.science.grade}")
    private String sqlAddScienceGrade;

    @Value("${sql.script.create.history.grade}")
    private String sqlAddHistoryGrade;

    @Value("${sql.script.delete.student}")
    private String sqlDeleteStudent;

    @Value("${sql.script.delete.math.grade}")
    private String sqlDeleteMathGrade;

    @Value("${sql.script.delete.science.grade}")
    private String sqlDeleteScienceGrade;

    @Value("${sql.script.delete.history.grade}")
    private String sqlDeleteHistoryGrade;

    public static final MediaType APPLICATION_JSON_UTF8 = MediaType.APPLICATION_JSON;

    @BeforeAll
    static void setup() {
        request = new MockHttpServletRequest();

        request.setParameter("firstname", "José Manuel");
        request.setParameter("lastname", "Muñoz");
        request.setParameter("emailAddress", "jmunoz@gmail.com");
    }

    @BeforeEach
    public void setupDatabase() {
        jdbc.execute(sqlAddStudent);
        jdbc.execute(sqlAddMathGrade);
        jdbc.execute(sqlAddScienceGrade);
        jdbc.execute(sqlAddHistoryGrade);
    }

    @Test
    void getStudentsHttpRequest() throws Exception {
        // Añadimos un segundo estudiante
        student.setFirstname("Adri");
        student.setLastname("Acosta");
        student.setEmailAddress("adri@gmail.com");
        entityManager.persist(student);
        // Para obligar a persistir los datos inmediatamente
        entityManager.flush();

        // Confirmamos que se devuelve un status 200 y un contenido JSON
        // y que el JSON es un array de 2 elementos
        mockMvc.perform(MockMvcRequestBuilders.get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void createStudentHttpRequest() throws Exception {
        // Estudiante a crear
        student.setFirstname("Adri");
        student.setLastname("Acosta");
        student.setEmailAddress("adri@gmail.com");

        // ObjectMapper pertenece al API Jackson
        // writeValueAsString sirve para generar una cadena de texto JSON a partir del objeto Java y lo incluye
        // en el cuerpo del HTTP request
        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // Doble check sobre el backend
        // Hacemos uso del DAO y buscamos el estudiante por su email
        CollegeStudent verifyStudent = studentDao.findByEmailAddress("adri@gmail.com");
        assertNotNull(verifyStudent, "Student should be valid");
    }

    @Test
    void deleteStudent() throws Exception {
        // Nos aseguramos que el estudiante existe antes de borrarlo
        assertTrue(studentDao.findById(1).isPresent());

        // Hacemos una petición DELETE
        // Esperamos un status de 200, un contenido JSON y que ese JSON tenga 0 estudiantes
        mockMvc.perform(MockMvcRequestBuilders.delete("/student/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", hasSize(0)));

        // Esperamos no encontrar el estudiante, ya que se acaba de borrar
        assertFalse(studentDao.findById(1).isPresent());
    }

    @Test
    void deleteStudentHttpRequestErrorPage() throws Exception {
        // Nos aseguramos que el estudiante con id 0 no existe antes de intentar borrarlo
        assertFalse(studentDao.findById(0).isPresent());

        // Intentamos borrar un estudiante con id inexistente
        // Esperamos un status 4xx
        // Y buscando en el JSON devuelto, buscamos un status 404 y un mensaje
        // Recordar que los errores envian los campos: status, message, timeStamp
        // Para el método is() hacer el import siguiente:
        // import static org.hamcrest.Matchers.*;
        mockMvc.perform(MockMvcRequestBuilders.delete("/student/{id}", 0))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Student or Grade was not found")));
    }

    @Test
    void studentInformationHttpRequest() throws Exception {
        // Recuperar información del estudiante

        // Nos aseguramos que el estudiante existe
        Optional<CollegeStudent> student = studentDao.findById(1);
        assertTrue(student.isPresent());

        // Petición GET para obtener datos del estudiante
        // Esperamos un estado 200 y obtener contenido en formato JSON
        // Se indica cuál es el contenido JSON de la respuesta esperado
        mockMvc.perform(MockMvcRequestBuilders.get("/studentInformation/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.firstname", is("Eric")))
                .andExpect(jsonPath("$.lastname", is("Roby")))
                .andExpect(jsonPath("$.emailAddress", is("eric.roby@luv2code_school.com")));
    }

    @Test
    void studentInformationHttpRequestEmptyResponse() throws Exception {
        // Nos aseguramos que el estudiante con id 0 no existe antes de intentar recuperar su información
        Optional<CollegeStudent> student = studentDao.findById(0);
        assertFalse(student.isPresent());

        // Intentamos un GET de su información.
        // Esperamos error 404, ya que el id del estudiante 0 no existe
        mockMvc.perform(MockMvcRequestBuilders.get("/studentInformation/{id}", 0))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Student or Grade was not found")));
    }

    @Test
    void createAValidGradeHttpRequest() throws Exception {
        // Se hace un POST pasando grade, gradeType y studentId
        // Esperamos un status 200 y un contenido JSON
        // El número de notas de la asignatura de matemáticas es 2 porque ya teníamos uno y hemos añadido otro
        this.mockMvc.perform(post("/grades")
            .contentType(MediaType.APPLICATION_JSON)
            .param("grade", "85.00")
            .param("gradeType", "math")
            .param("studentId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.firstname", is("Eric")))
                .andExpect(jsonPath("$.lastname", is("Roby")))
                .andExpect(jsonPath("$.emailAddress", is("eric.roby@luv2code_school.com")))
                .andExpect(jsonPath("$.studentGrades.mathGradeResults", hasSize(2)));
    }

    @Test
    void createAValidGradeHttpRequestStudentDoesNotExistEmptyResponse() throws Exception {
        // Hacer un POST pasando un id de estudiante que no existe
        // Esperamos un status 404 y un mensaje de error
        this.mockMvc.perform(post("/grades")
            .contentType(MediaType.APPLICATION_JSON)
            .param("grade", "85.00")
            .param("gradeType", "math")
            .param("studentId", "0"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Student or Grade was not found")));
    }

    @Test
    void createANonValidGradeHttpRequestGradeTypeDoesNotExistEmptyResponse() throws Exception {
        // Hacer un POST a un grade inexistente
        this.mockMvc.perform(post("/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("grade", "85.00")
                        .param("gradeType", "literature")
                        .param("studentId", "1"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Student or Grade was not found")));
    }

    @Test
    void deleteAValidGradeHttpRequest() throws Exception {
        // Verificamos que el grade existe antes de borrarlo
        Optional<MathGrade> mathGrade = mathGradeDao.findById(1);
        assertTrue(mathGrade.isPresent());

        // Hacemos el delete
        // Esperamos un status 200
        // Y un contenido JSON con los datos del estudiante y ningún grade, ya que se ha borrado
        mockMvc.perform(MockMvcRequestBuilders.delete("/grades/{id}/{gradeType}", 1, "math"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.firstname", is("Eric")))
                .andExpect(jsonPath("$.lastname", is("Roby")))
                .andExpect(jsonPath("$.emailAddress", is("eric.roby@luv2code_school.com")))
                .andExpect(jsonPath("$.studentGrades.mathGradeResults", hasSize(0)));
    }

    @Test
    void deleteAValidGradeHttpRequestStudentIdDoesNotExistEmptyResponse() throws Exception {
        // Intentamos hacer un delete de un grade de un estudiante cuyo id no existe
        // Esperamos un status 404 y un mensaje de error
        mockMvc.perform(MockMvcRequestBuilders.delete("/grades/{id}/{gradeType}", 2, "history"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Student or Grade was not found")));
    }

    @AfterEach
    public void setupAfterTransaction() {
        jdbc.execute(sqlDeleteStudent);
        jdbc.execute(sqlDeleteMathGrade);
        jdbc.execute(sqlDeleteScienceGrade);
        jdbc.execute(sqlDeleteHistoryGrade);
    }

}
