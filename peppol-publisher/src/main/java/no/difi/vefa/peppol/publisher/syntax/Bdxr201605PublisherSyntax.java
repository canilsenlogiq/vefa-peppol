package no.difi.vefa.peppol.publisher.syntax;

import no.difi.commons.bdx.jaxb.smp._2016._05.*;
import no.difi.vefa.peppol.common.api.PerformResult;
import no.difi.vefa.peppol.common.model.DocumentTypeIdentifier;
import no.difi.vefa.peppol.common.model.ParticipantIdentifier;
import no.difi.vefa.peppol.common.model.ProcessIdentifier;
import no.difi.vefa.peppol.common.model.ProcessMetadata;
import no.difi.vefa.peppol.common.util.ExceptionUtil;
import no.difi.vefa.peppol.publisher.annotation.Syntax;
import no.difi.vefa.peppol.publisher.api.PublisherSyntax;
import no.difi.vefa.peppol.publisher.model.PublisherEndpoint;
import no.difi.vefa.peppol.publisher.model.PublisherServiceMetadata;
import no.difi.vefa.peppol.publisher.model.ServiceGroup;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.URI;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * @author erlend
 */
@Syntax({"bdxr", "bdxr-201605"})
public class Bdxr201605PublisherSyntax implements PublisherSyntax {

    private static final DatatypeFactory DATATYPE_FACTORY =
            ExceptionUtil.perform(IllegalStateException.class, new PerformResult<DatatypeFactory>() {
                @Override
                public DatatypeFactory action() throws Exception {
                    return DatatypeFactory.newInstance();
                }
            });

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    private static final JAXBContext JAXB_CONTEXT =
            ExceptionUtil.perform(IllegalStateException.class, new PerformResult<JAXBContext>() {
                @Override
                public JAXBContext action() throws Exception {
                    return JAXBContext.newInstance(ServiceGroupType.class,
                            ServiceMetadataType.class, SignedServiceMetadataType.class);
                }
            });

    @SuppressWarnings("all")
    @Override
    public JAXBElement<?> of(ServiceGroup serviceGroup, URI rootUri) {
        ServiceGroupType serviceGroupType = new ServiceGroupType();
        serviceGroupType.setParticipantIdentifier(convert(serviceGroup.getParticipantIdentifier()));
        serviceGroupType.setServiceMetadataReferenceCollection(new ServiceMetadataReferenceCollectionType());

        for (DocumentTypeIdentifier documentTypeIdentifier : serviceGroup.getDocumentTypeIdentifiers())
            serviceGroupType
                    .getServiceMetadataReferenceCollection()
                    .getServiceMetadataReference()
                    .add(convertRef(serviceGroup.getParticipantIdentifier(), documentTypeIdentifier, rootUri));

        return OBJECT_FACTORY.createServiceGroup(serviceGroupType);
    }

    @SuppressWarnings("all")
    @Override
    public JAXBElement<?> of(PublisherServiceMetadata serviceMetadata, boolean forSigning) {
        ServiceInformationType serviceInformationType = new ServiceInformationType();
        serviceInformationType.setParticipantIdentifier(convert(serviceMetadata.getParticipantIdentifier()));
        serviceInformationType.setDocumentIdentifier(convert(serviceMetadata.getDocumentTypeIdentifier()));
        serviceInformationType.setProcessList(new ProcessListType());

        for (ProcessMetadata processMetadata : serviceMetadata.getProcesses())
            serviceInformationType.getProcessList().getProcess().add(convert(processMetadata));

        ServiceMetadataType serviceMetadataType = new ServiceMetadataType();
        serviceMetadataType.setServiceInformation(serviceInformationType);

        if (forSigning) {
            SignedServiceMetadataType signedServiceMetadataType = new SignedServiceMetadataType();
            signedServiceMetadataType.setServiceMetadata(serviceMetadataType);
            return OBJECT_FACTORY.createSignedServiceMetadata(signedServiceMetadataType);
        } else {
            return OBJECT_FACTORY.createServiceMetadata(serviceMetadataType);
        }
    }

    @Override
    public Marshaller getMarshaller() throws JAXBException {
        return JAXB_CONTEXT.createMarshaller();
    }

    private ParticipantIdentifierType convert(ParticipantIdentifier participantIdentifier) {
        ParticipantIdentifierType participantIdentifierType = new ParticipantIdentifierType();
        participantIdentifierType.setScheme(participantIdentifier.getScheme().getValue());
        participantIdentifierType.setValue(participantIdentifier.getIdentifier());
        return participantIdentifierType;
    }

    private ProcessIdentifierType convert(ProcessIdentifier processIdentifier) {
        ProcessIdentifierType processIdentifierType = new ProcessIdentifierType();
        processIdentifierType.setScheme(processIdentifier.getScheme().getValue());
        processIdentifierType.setValue(processIdentifier.getIdentifier());
        return processIdentifierType;
    }

    private DocumentIdentifierType convert(DocumentTypeIdentifier documentTypeIdentifier) {
        DocumentIdentifierType documentIdentifierType = new DocumentIdentifierType();
        documentIdentifierType.setScheme(documentTypeIdentifier.getScheme().getValue());
        documentIdentifierType.setValue(documentTypeIdentifier.getIdentifier());
        return documentIdentifierType;
    }

    @SuppressWarnings("all")
    private ProcessType convert(ProcessMetadata<PublisherEndpoint> processMetadata) {
        ProcessType processType = new ProcessType();
        processType.setProcessIdentifier(convert(processMetadata.getProcessIdentifier()));
        processType.setServiceEndpointList(new ServiceEndpointList());

        for (PublisherEndpoint endpoint : processMetadata.getEndpoints())
            processType.getServiceEndpointList().getEndpoint().add(convert(endpoint));

        return processType;
    }

    private EndpointType convert(PublisherEndpoint endpoint) {
        EndpointType endpointType = new EndpointType();
        endpointType.setTransportProfile(endpoint.getTransportProfile().getValue());
        endpointType.setRequireBusinessLevelSignature(false);
        endpointType.setEndpointURI(endpoint.getAddress().toString());
        endpointType.setServiceActivationDate(convert(endpoint.getActivationDate()));
        endpointType.setServiceExpirationDate(convert(endpoint.getExpirationDate()));
        endpointType.setCertificate(endpoint.getCertificate());
        endpointType.setServiceDescription(endpoint.getDescription());
        endpointType.setTechnicalContactUrl(endpoint.getTechnicalContact());

        return endpointType;
    }

    private XMLGregorianCalendar convert(final Date date) {
        if (date == null)
            return null;

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(date);
        return DATATYPE_FACTORY.newXMLGregorianCalendar(gregorianCalendar);
    }

    private ServiceMetadataReferenceType convertRef(ParticipantIdentifier participantIdentifier,
                                                    DocumentTypeIdentifier documentTypeIdentifier, URI rootURI) {
        URI uri = rootURI.resolve(String.format("%s/services/%s",
                participantIdentifier.urlencoded(), documentTypeIdentifier.urlencoded()));

        ServiceMetadataReferenceType serviceMetadataReferenceType = new ServiceMetadataReferenceType();
        serviceMetadataReferenceType.setHref(uri.toString());
        return serviceMetadataReferenceType;
    }
}