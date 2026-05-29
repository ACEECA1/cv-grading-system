package org.djezzy.pfe.service.job;

import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.dao.evaluation.CVDAO;
import org.djezzy.pfe.dao.job.JobOfferDAO;
import org.djezzy.pfe.dao.job.StructuredJdDAO;
import org.djezzy.pfe.dto.job.CreateJobOfferRequest;
import org.djezzy.pfe.dto.job.JobOfferDTO;
import org.djezzy.pfe.model.auth.User;
import org.djezzy.pfe.model.job.JobOffer;
import org.djezzy.pfe.model.job.JobOfferStatus;
import org.djezzy.pfe.util.MapperUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobOfferServiceTest {

    @Mock
    private CVDAO cvdao;

    @Mock
    private JobOfferDAO jobOfferDAO;

    @Mock
    private StructuredJdDAO structuredJdDAO;

    @Mock
    private LlmParsingService llmParsingService;

    @Mock
    private MapperUtil mapperUtil;

    @Mock
    private AppProperties appProperties;

    @Mock
    private ObjectProvider<JobOfferService> selfProvider;

    @InjectMocks
    private JobOfferService jobOfferService;

    @Test
    void createJobOffer_Success() {
        CreateJobOfferRequest request = new CreateJobOfferRequest("Title", "Raw text");
        User user = new User();
        user.setId(1L);
        JobOfferDTO expectedDto = new JobOfferDTO(1L, "Title", null, JobOfferStatus.DRAFT, null, null, Instant.now(), null);

        when(mapperUtil.toJobOfferDto(any())).thenReturn(expectedDto);
        when(selfProvider.getObject()).thenReturn(jobOfferService);

        JobOfferDTO result = jobOfferService.createJobOffer(request, user);

        assertNotNull(result);
        assertEquals("Title", result.title());
        verify(jobOfferDAO).save(any(JobOffer.class));
    }

    @Test
    void listPublicJobOffers_Success() {
        JobOffer offer = new JobOffer();
        offer.setId(1L);
        offer.setStatus(JobOfferStatus.PUBLISHED);
        when(jobOfferDAO.findByStatus(JobOfferStatus.PUBLISHED)).thenReturn(List.of(offer));
        
        JobOfferDTO expectedDto = new JobOfferDTO(1L, "Title", null, JobOfferStatus.PUBLISHED, null, null, Instant.now(), null);
        when(mapperUtil.toJobOfferDto(offer)).thenReturn(expectedDto);

        List<JobOfferDTO> results = jobOfferService.listPublicJobOffers();
        assertEquals(1, results.size());
    }

    @Test
    void deleteJobOffer_Success() {
        JobOffer offer = new JobOffer();
        offer.setId(1L);
        when(jobOfferDAO.findById(1L)).thenReturn(Optional.of(offer));

        jobOfferService.deleteJobOffer(1L);
        verify(jobOfferDAO).delete(offer);
    }
}
