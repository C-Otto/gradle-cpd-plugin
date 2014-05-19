package de.aaschmid.gradle.plugins.cpd.internal

import de.aaschmid.gradle.plugins.cpd.test.BaseSpec
import net.sourceforge.pmd.cpd.CSVRenderer
import net.sourceforge.pmd.cpd.Match
import net.sourceforge.pmd.cpd.SimpleRenderer
import net.sourceforge.pmd.cpd.TokenEntry
import net.sourceforge.pmd.cpd.XMLRenderer
import org.gradle.api.InvalidUserDataException

import java.nio.file.Files

class CpdReporterTest extends BaseSpec {

    def underTest

    def setup() {
        underTest = new CpdReporter(project.tasks.findByName('cpd'))
    }

    def "test 'new CpdReporter(null)' should throw 'NullPointerException'"() {
        when:
        new CpdReporter(null)

        then:
        def e = thrown NullPointerException
        e.getMessage() ==~ /task must not be null/
    }

    def "test 'canGenerate()' should throw 'InvalidUserDataException' if encoding is 'null'"() {
        given:
        underTest.task.encoding = null

        when:
        underTest.canGenerate()

        then:
        def e = thrown InvalidUserDataException
        e.getMessage() ==~ /Task '.+' requires encoding but was: null./
    }

    def "test 'canGenerate()' should throw 'InvalidUserDataException' if no report is enabled"() {
        given:
        tasks.cpd.reports{
            csv.enabled = false
            text.enabled = false
            xml.enabled = false
        }

        when:
        underTest.canGenerate()

        then:
        def e = thrown InvalidUserDataException
        e.getMessage() ==~ /Task '.+' requires exactly one report to be enabled but was: \[\]\./
    }

    def "test 'canGenerate()' should throw 'InvalidUserDataException' if two reports are enabled"() {
        given:
        tasks.cpd.reports{
            csv.enabled = true
            text.enabled = false
            xml.enabled = true
        }

        when:
        underTest.canGenerate()

        then:
        def e = thrown InvalidUserDataException
        e.getMessage() ==~ /Task '.+' requires exactly one report to be enabled but was: \[csv, xml\]\./
    }

    def "test 'canGenerate()' should throw 'InvalidUserDataException' if destination of enabled report is 'null'"() {
        given:
        tasks.cpd.reports{
            csv{
                enabled = true
                destination = null
            }
            text.enabled = false
            xml.enabled = false
        }

        when:
        underTest.canGenerate()

        then:
        def e = thrown InvalidUserDataException
        e.getMessage() ==~ /'.*csv' requires valid destination but was 'null'\./
    }

    def "test 'canGenerate()' should not throw 'InvalidUserDataException' for default task arguments"() {
        when:
        underTest.canGenerate()

        then:
        notThrown InvalidUserDataException
    }

    def "test 'createRendererFor(...)' should return 'CsvRenderer' with default 'separator'"() {
        when:
        def result = underTest.createRendererFor(tasks.cpd.reports.csv)

        then:
        result instanceof CSVRenderer
        result.separator == ','
    }

    def "test 'createRendererFor(...)' should return 'CsvRenderer' with set 'separator'"() {
        given:
        tasks.cpd.reports.csv.separator = ';'

        when:
        def result = underTest.createRendererFor(tasks.cpd.reports.csv)

        then:
        result instanceof CSVRenderer
        result.separator == ';'
    }

    def "test 'createRendererFor(...)' should return 'SimpleRenderer' with default 'lineSeparator' and 'trimLeadingCommonSourceWhitespaces'"() {
        when:
        def result = underTest.createRendererFor(tasks.cpd.reports.text)

        then:
        result instanceof SimpleRenderer
        result.separator == '====================================================================='
        result.trimLeadingWhitespace == false
    }

    def "test 'createRendererFor(...)' should return 'SimpleRenderer' with set 'lineSeparator'"() {
        given:
        tasks.cpd.reports.text.lineSeparator = '---------------------------------------------------------------------'

        when:
        def result = underTest.createRendererFor(tasks.cpd.reports.text)

        then:
        result instanceof SimpleRenderer
        result.separator == '---------------------------------------------------------------------'
    }

    def "test 'createRendererFor(...)' should return 'SimpleRenderer' with set 'trimLeadingCommonSourceWhitespaces'"() {
        given:
        tasks.cpd.reports.text.trimLeadingCommonSourceWhitespaces = true

        when:
        def result = underTest.createRendererFor(tasks.cpd.reports.text)

        then:
        result instanceof SimpleRenderer
        result.trimLeadingWhitespace == true
    }

    def "test 'createRendererFor(...)' should return 'SimpleRenderer' with set 'lineSeparator' and 'trimLeadingCommonSourceWhitespaces'"() {
        given:
        tasks.cpd.reports.text{
            lineSeparator = '/////////////////////////////////////////////////////////////////////'
            trimLeadingCommonSourceWhitespaces = true
        }

        when:
        def result = underTest.createRendererFor(tasks.cpd.reports.text)

        then:
        result instanceof SimpleRenderer
        result.separator == '/////////////////////////////////////////////////////////////////////'
        result.trimLeadingWhitespace == true
    }

    def "test 'createRendererFor(...)' should return 'XmlRenderer' with set 'encoding'"() {
        given:
        tasks.cpd.reports.xml.encoding = 'ISO-8859-1'

        when:
        def result = underTest.createRendererFor(tasks.cpd.reports.xml)

        then:
        result instanceof XMLRenderer
        result.encoding == 'ISO-8859-1'
    }

    def "test 'createRendererFor(...)' should return 'XmlRenderer' with task 'encoding'"() {
        given:
        tasks.cpd {
            encoding = 'US-ASCII'
            reports.xml.encoding = null
        }

        when:
        def result = underTest.createRendererFor(tasks.cpd.reports.xml)

        then:
        result instanceof XMLRenderer
        result.encoding == 'US-ASCII'
    }

    def "test 'generate' should ..."() { // TODO more and better tests or let is be as acceptance test? otherweise also do for executor => integration test
        given:
        tasks.cpd.reports{
            text{
                lineSeparator = "----------------------"
                enabled = true
                destination = Files.createTempFile("test", "text")
            }
        }

        def tokenEntry1 = new TokenEntry('1', 'Clazz1.java', 5)
        def tokenEntry2 = new TokenEntry('2', 'Clazz2.java', 7)
        def match = new Match(5, tokenEntry1, tokenEntry2)

        when:
        underTest.generate([ match, match ])

        then:
        tasks.cpd.reports.text.destination.text
    }
}
