package com.liferay.appointments.internal.resource.v1_0;

import com.liferay.appointments.dto.v1_0.Appointment;
import com.liferay.appointments.resource.v1_0.AppointmentResource;
import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.model.LocalizedValue;
import com.liferay.dynamic.data.mapping.service.DDMStructureService;
import com.liferay.dynamic.data.mapping.storage.DDMFormFieldValue;
import com.liferay.dynamic.data.mapping.storage.DDMFormValues;
import com.liferay.dynamic.data.mapping.storage.Fields;
import com.liferay.dynamic.data.mapping.util.DDM;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.service.JournalArticleLocalService;
import com.liferay.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.journal.service.JournalArticleService;
import com.liferay.journal.util.JournalConverter;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.portal.vulcan.util.LocalDateTimeUtil;

import java.text.SimpleDateFormat;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * @author Javier Gamarra
 */
@Component(
	properties = "OSGI-INF/liferay/rest/v1_0/appointment.properties",
	scope = ServiceScope.PROTOTYPE, service = AppointmentResource.class
)
public class AppointmentResourceImpl extends BaseAppointmentResourceImpl {

	@Override
	public Page<Appointment> getSiteAppointmentsPage(Long siteId)
		throws Exception {

		List<JournalArticle> articles =
			_journalArticleService.getArticles(
				siteId, 0, contextAcceptLanguage.getPreferredLocale());

		List<Appointment> appointments = new ArrayList<>(articles.size());

		for (JournalArticle article : articles) {
			appointments.add(_toAppointment(article));
		}

		return Page.of(appointments);
	}

	@Override
	public Appointment postSiteAppointment(Long siteId, Appointment appointment)
		throws Exception {

		JournalArticle journalArticle = _createJournalArticle(
			siteId, appointment);

		return _toAppointment(journalArticle);
	}

	private JournalArticle _createJournalArticle(
			long siteId, Appointment appointment)
		throws Exception {

		DDMStructure ddmStructure = _ddmStructureService.getStructure(
			siteId, _portal.getClassNameId(JournalArticle.class),
			"BASIC-WEB-CONTENT", true);

		SimpleDateFormat simpleDateFormat = getDateFormat();

		DDMForm ddmForm = ddmStructure.getDDMForm();

		LocalizedValue localizedValue = new LocalizedValue();

		localizedValue.addString(
			contextAcceptLanguage.getPreferredLocale(),
			simpleDateFormat.format(appointment.getDate()));

		DDMFormFieldValue ddmFormFieldValue = new DDMFormFieldValue();

		ddmFormFieldValue.setName("content");
		ddmFormFieldValue.setValue(localizedValue);

		DDMFormValues ddmFormValues = new DDMFormValues(ddmForm);

		ddmFormValues.setAvailableLocales(ddmForm.getAvailableLocales());
		ddmFormValues.setDDMFormFieldValues(Arrays.asList(ddmFormFieldValue));
		ddmFormValues.setDefaultLocale(ddmForm.getDefaultLocale());

		Fields fields = _ddm.getFields(
			ddmStructure.getStructureId(), ddmFormValues);

		LocalDateTime localDateTime = LocalDateTimeUtil.toLocalDateTime(
			new Date());
		String content = _journalConverter.getContent(ddmStructure, fields);

		HashMap<Locale, String> titleMap = new HashMap<>();

		titleMap.put(
			contextAcceptLanguage.getPreferredLocale(), appointment.getTitle());

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setScopeGroupId(siteId);

		return _journalArticleService.addArticle(
			siteId, 0, 0, 0, null, true, titleMap, null, content,
			ddmStructure.getStructureKey(), null, null,
			localDateTime.getMonthValue() - 1, localDateTime.getDayOfMonth(),
			localDateTime.getYear(), localDateTime.getHour(),
			localDateTime.getMinute(), 0, 0, 0, 0, 0, true, 0, 0, 0, 0, 0, true,
			true, null, serviceContext);
	}

	private Appointment _toAppointment(JournalArticle journalArticle)
		throws Exception {

		Appointment appointment = new Appointment();

		Fields fields = _journalConverter.getDDMFields(
			journalArticle.getDDMStructure(), journalArticle.getContent());

		appointment.setTitle(
			journalArticle.getTitle(
				contextAcceptLanguage.getPreferredLocale()));

		SimpleDateFormat simpleDateFormat = getDateFormat();

		appointment.setDate(
			simpleDateFormat.parse(
				(String)fields.get(
					"content"
				).getValue()));

		return appointment;
	}

	private SimpleDateFormat getDateFormat() {
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	}

	@Reference
	private DDM _ddm;

	@Reference
	private DDMStructureService _ddmStructureService;

	@Reference
	private JournalArticleService _journalArticleService;

	@Reference
	private JournalConverter _journalConverter;

	@Reference
	private Portal _portal;

}